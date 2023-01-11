package com.example.cloudplaylistmanager.MusicPlayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class MediaPlayerActivity extends AppCompatActivity {
    public static final String SERIALIZE_TAG = "data";
    public static final String POSITION_TAG = "position";
    public static final String SHUFFLED_TAG = "shuffle";
    private static final int NOTIFICATION_UPDATE_LIMITER = 4;

    private ImageView mediaPlayerIcon;
    private TextView mediaPlayerTitle;
    private SeekBar mediaPlayerSeekBar;
    private TextView mediaPlayerCurrentTime;
    private TextView mediaPlayerDurationTime;
    private ImageView mediaPlayerRepeat;
    private ImageView mediaPlayerSkipPrevious;
    private ImageView mediaPlayerPlay;
    private ImageView mediaPlayerSkipNext;
    private ImageView mediaPlayerShuffle;
    private TextView mediaPlayerPlaylistTitle;

    private PlaylistInfo playlistInfo;
    RecyclerView songRecyclerView;
    private SongsRecyclerAdapter songsAdapter;
    private int startingPosition;
    private boolean shuffled;
    private int rateLimit = 0;
    private MusicService musicService = null;
    private MediaSessionCompat mediaSession;
    private ServiceConnection musicServiceConnection;
    private Handler handler;
    private BroadcastReceiver updateNotificationReciver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        //Sets action bar style.
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //Receives the data that was sent to by the calling Activity.
        this.playlistInfo = (PlaylistInfo) getIntent().getSerializableExtra(SERIALIZE_TAG);
        this.shuffled = getIntent().getBooleanExtra(SHUFFLED_TAG,false);
        if(this.playlistInfo == null || this.playlistInfo.getAllVideos() == null || this.playlistInfo.getAllVideos().isEmpty()) {
            this.finish();
            return;
        }
        if(this.shuffled) {
            Random ran = new Random();
            this.startingPosition = ran.nextInt(this.playlistInfo.getAllVideos().size());
        }
        else {
            this.startingPosition = getIntent().getIntExtra(POSITION_TAG,-1);
        }


        this.handler = new Handler();
        this.rateLimit = 0;


        //Binds media player service
        this.musicServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d("MusicService","Service Connected...");
                MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) iBinder;
                musicService = binder.getBinder();
                if(playlistInfo != null) {
                    ArrayList<PlaybackAudioInfo> allSongs = new ArrayList<>();
                    allSongs.addAll(playlistInfo.getAllVideos());
                    startingPosition = startingPosition % allSongs.size();
                    StartMediaPlayer(allSongs);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                musicService = null;
                Log.d("MusicService","Service Disconnected");
            }
        };
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, this.musicServiceConnection, Context.BIND_AUTO_CREATE);

        this.mediaSession = new MediaSessionCompat(this, "PlayerAudio");

        //Initiates all of the items in the view.
        this.mediaPlayerIcon = findViewById(R.id.mediaPlayer_icon);
        this.mediaPlayerTitle = findViewById(R.id.mediaPlayer_title);
        this.mediaPlayerSeekBar = findViewById(R.id.seekBar_mediaPlayer);
        this.mediaPlayerCurrentTime = findViewById(R.id.mediaPlayer_currentTime);
        this.mediaPlayerDurationTime = findViewById(R.id.mediaPlayer_durationTime);
        this.mediaPlayerRepeat = findViewById(R.id.mediaPlayer_repeat);
        this.mediaPlayerSkipPrevious = findViewById(R.id.mediaPlayer_skip_previous);
        this.mediaPlayerPlay = findViewById(R.id.mediaPlayer_play);
        this.mediaPlayerSkipNext = findViewById(R.id.mediaPlayer_skip_next);
        this.mediaPlayerShuffle = findViewById(R.id.mediaPlayer_shuffle);
        this.mediaPlayerPlaylistTitle = findViewById(R.id.mediaPlayer_playlistTitle);
        this.songRecyclerView = findViewById(R.id.mediaPlayer_recyclerView);


        this.mediaPlayerPlaylistTitle.setText(this.playlistInfo.getTitle());

        //Initiates the click listeners on the UI.
        this.mediaPlayerPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlayClicked();
            }
        });

        this.mediaPlayerSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextClicked();
            }
        });

        this.mediaPlayerSkipPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPrevClicked();
            }
        });

        this.mediaPlayerShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.SetShuffle(!musicService.IsShuffle());
                }
            }
        });

        this.mediaPlayerRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.SetRepeat(!musicService.IsRepeat());
                }
            }
        });

        this.mediaPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(musicService != null && b) {
                    musicService.SeekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        //Sets receiver for the notification broadcast.
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationReceiver.MEDIA_NOTIFICATION_ACTION);
        this.updateNotificationReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    switch(intent.getStringExtra(NotificationReceiver.MEDIA_NOTIFICATION_ACTION_KEY)) {
                        case ApplicationClass.ACTION_PLAY:
                            onPlayClicked();
                            break;
                        case ApplicationClass.ACTION_NEXT:
                            onNextClicked();
                            break;
                        case ApplicationClass.ACTION_PREV:
                            onPrevClicked();
                    }
                }
            }
        };
        registerReceiver(this.updateNotificationReciver, filter);


        //Instantiates new Recycler View and sets the adapter.
        this.songRecyclerView.setHasFixedSize(true);
        this.songRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Implement a listener to capture clicks on the recycler view items.
        this.songsAdapter = new SongsRecyclerAdapter(this, this.playlistInfo, false, new RecyclerViewItemClickedListener() {
            @Override
            public void onClicked(int viewType, int position) {
                if(viewType != SongsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    if(musicService.GetCurrentSongIndex() != position) {
                        musicService.SwitchSong(position);
                    }
                    Log.d("MediaPlayer","Recycler Clicked!");
                }
            }
        });
        this.songRecyclerView.setAdapter(this.songsAdapter);
    }


    public void StartMediaPlayer(ArrayList<PlaybackAudioInfo> allSongs) {
        this.musicService.InitializePlayer(allSongs,new OnUpdatePlayerListener() {
            @Override
            public void onPauseUpdate(boolean isPaused) {
                if(musicService.IsPaused()) {
                    mediaPlayerPlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
                } else {
                    mediaPlayerPlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                }
            }

            @Override
            public void onShuffleUpdate(boolean isShuffled) {
                if(isShuffled) {
                    ImageViewCompat.setImageTintList(mediaPlayerShuffle, ColorStateList.valueOf(FetchColor(R.attr.colorPrimary)));
                } else {
                    ImageViewCompat.setImageTintList(mediaPlayerShuffle, ColorStateList.valueOf(FetchColor(R.attr.colorSecondary)));
                }
            }

            @Override
            public void onRepeatUpdate(boolean isRepeat) {
                if(isRepeat) {
                    ImageViewCompat.setImageTintList(mediaPlayerRepeat, ColorStateList.valueOf(FetchColor(R.attr.colorPrimary)));
                } else {
                    ImageViewCompat.setImageTintList(mediaPlayerRepeat, ColorStateList.valueOf(FetchColor(R.attr.colorSecondary)));
                }
            }

            @Override
            public void onSongChange(PlaybackAudioInfo audio, int position) {
                mediaPlayerTitle.setText(audio.getTitle());
                mediaPlayerIcon.setImageBitmap(GetMediaPlayerIcon(audio));

                if(songRecyclerView != null && songRecyclerView.getLayoutManager() != null) {
                    songRecyclerView.getLayoutManager().scrollToPosition(position);
                }
            }

            @Override
            public void onPrepared(int duration) {
                mediaPlayerSeekBar.setMax(duration);
                mediaPlayerDurationTime.setText(ConvertTimeUnitsToString(duration));
            }

            @Override
            public void onEnd() {
                if(!isFinishing()) {
                    finish();
                }
            }
        });


        //Begin Playing the song.
        this.musicService.BeginPlaying(this.startingPosition,this.shuffled,true);

        //Updates the Seek Bar
        this.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (musicService != null) {
                        mediaPlayerSeekBar.setProgress(musicService.GetCurrentPosition());
                        mediaPlayerCurrentTime.setText(ConvertTimeUnitsToString(musicService.GetCurrentPosition()));
                        ShowNotification(); //Updates notification.
                    }
                } catch(Exception e) {
                    mediaPlayerSeekBar.setProgress(0);
                    mediaPlayerCurrentTime.setText(ConvertTimeUnitsToString(0));
                }
                handler.postDelayed(this,100);
            }
        },0);

    }

    public void onPlayClicked() {
        if(this.musicService != null) {
            if(this.musicService.IsPaused()) {
                this.musicService.Resume();
            } else {
                this.musicService.Pause();
            }
        }
    }

    public void onNextClicked() {
        if(this.musicService != null) {
            this.musicService.SwitchSong(MusicService.NEXT_SONG_IGNORED);
        }
    }

    public void onPrevClicked() {
        if(musicService != null) {
            musicService.SwitchSong(MusicService.NEXT_SONG_PREV);
        }
    }


    public Bitmap GetMediaPlayerIcon(PlaybackAudioInfo audio) {
        if(audio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
            Bitmap bitmap = BitmapFactory.decodeFile(audio.getThumbnailSource());
            if (bitmap != null) {
                return bitmap;
            }
            else {
                return BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.med_res);
            }
        }
        else {
            return BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.med_res);
        }
    }

    public static String ConvertTimeUnitsToString(int time) {
        return String.format(Locale.US,"%02d:%02d",TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));
    }

    public int FetchColor(int attribute) {
        TypedArray a = this.obtainStyledAttributes(new TypedValue().data, new int[] { attribute });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(this.handler != null) {
            this.handler.removeCallbacksAndMessages(null);
        }
        if(this.updateNotificationReciver != null) {
            unregisterReceiver(this.updateNotificationReciver);
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);

        if(this.musicService != null) {
            //Stops the music service.
            Log.d("MusicService","Service Unbinding...");
            unbindService(this.musicServiceConnection);
        }
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void ShowNotification() {
        if(this.musicService == null || (this.rateLimit++) % NOTIFICATION_UPDATE_LIMITER != 0) {
            return;
        }

        int playIcon = R.drawable.ic_baseline_pause_circle_outline_24;
        if(this.musicService.IsPaused()) {
            playIcon = R.drawable.ic_baseline_play_circle_outline_24;
        }

        this.mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,-1)
                .build()
        );

        Intent intent = new Intent(this, MediaPlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent playIntent = new Intent(this, NotificationReceiver.class).setAction(ApplicationClass.ACTION_PLAY);
        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ApplicationClass.ACTION_NEXT);
        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ApplicationClass.ACTION_PREV);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this,0,playIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this,0,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this,0,prevIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        Bitmap image = GetMediaPlayerIcon(this.musicService.GetAudioInfo());

        Notification notification = new NotificationCompat.Builder(this, ApplicationClass.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_baseline_play_circle_outline_24)
                .setLargeIcon(image)
                .setContentTitle(this.musicService.GetAudioInfo().getTitle())
                .setContentText(ConvertTimeUnitsToString(this.musicService.GetCurrentPosition()) + " / " + ConvertTimeUnitsToString(this.musicService.GetDuration()))
                .addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", prevPendingIntent)
                .addAction(playIcon, "Play", playPendingIntent)
                .addAction(R.drawable.ic_baseline_skip_next_24, "Next", nextPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(this.mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }
}