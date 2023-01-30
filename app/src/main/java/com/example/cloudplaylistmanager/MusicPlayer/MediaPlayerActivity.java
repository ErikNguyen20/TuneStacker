package com.example.cloudplaylistmanager.MusicPlayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.Locale;
import java.util.concurrent.TimeUnit;


/**
 * Media Player Activity that serves as an interface for {@link MusicBrowserService}.
 * This Class handles all events between the UI, the media player, and the
 * popup notification.
 */
public class MediaPlayerActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MediaPlayerActivity";
    public static final String SERIALIZE_TAG = "data";
    public static final String POSITION_TAG = "position";
    public static final String SHUFFLED_TAG = "shuffle";

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

    private MusicBrowserService musicService = null;
    private ServiceConnection musicServiceConnection;
    private Handler handler;

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
            this.startingPosition = MusicPlayback.NEXT_SONG_IGNORED;
        }
        else {
            this.startingPosition = getIntent().getIntExtra(POSITION_TAG, MusicPlayback.NEXT_SONG_IGNORED);
        }
        this.handler = new Handler();


        //Binds media player service
        this.musicServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(LOG_TAG,"Service Connected...");
                MusicBrowserService.MusicServiceBinder binder = (MusicBrowserService.MusicServiceBinder) iBinder;
                musicService = binder.getBinder();

                if(playlistInfo != null) {
                    //Starts the Media Player.
                    startingPosition = startingPosition % playlistInfo.getAllVideos().size();
                    StartMediaPlayer();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                musicService = null;
                Log.d(LOG_TAG,"Service Disconnected");
            }
        };
        Intent serviceIntent = new Intent(this, MusicBrowserService.class);
        bindService(serviceIntent, this.musicServiceConnection, Context.BIND_AUTO_CREATE);


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
                if(musicService != null) {
                    musicService.ClientControlPlayButton();
                }
            }
        });
        this.mediaPlayerSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.ClientControlSwitchSong(MusicPlayback.NEXT_SONG_IGNORED);
                }
            }
        });
        this.mediaPlayerSkipPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.ClientControlSwitchSong(MusicPlayback.NEXT_SONG_PREV);
                }
            }
        });
        this.mediaPlayerShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.ClientControlShuffle();
                }
            }
        });
        this.mediaPlayerRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.ClientControlRepeat();
                }
            }
        });
        this.mediaPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(musicService != null && b) {
                    musicService.ClientControlSeekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        //Instantiates new Recycler View and sets the adapter.
        this.songRecyclerView.setHasFixedSize(true);
        this.songRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Implement a listener to capture clicks on the recycler view items.
        this.songsAdapter = new SongsRecyclerAdapter(this, this.playlistInfo, false, new RecyclerViewItemClickedListener() {
            @Override
            public void onClicked(int viewType, int position) {
                if(viewType != SongsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    if(musicService.GetCurrentSongIndex() != position) {
                        musicService.ClientControlSwitchSong(position);
                    }
                    Log.d(LOG_TAG,"Recycler Clicked!");
                }
            }
        });
        this.songRecyclerView.setAdapter(this.songsAdapter);
    }

    /**
     * Initializes and starts the Media Player. Also handles most
     * of the events of the media player.
     */
    public void StartMediaPlayer() {
        this.musicService.StartPlayer(this.playlistInfo, this.startingPosition, this.shuffled,
                true, new OnUpdatePlayerListener() {
            @Override
            public void onPauseUpdate(boolean isPaused) {
                if(isPaused) {
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

        //Updates the Seek Bar and Notification.
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (musicService != null) {
                                int pos = musicService.GetCurrentPosition();
                                mediaPlayerSeekBar.setProgress(pos);
                                mediaPlayerCurrentTime.setText(ConvertTimeUnitsToString(pos));
                            }
                        } catch(Exception e) {
                            mediaPlayerSeekBar.setProgress(0);
                            mediaPlayerCurrentTime.setText(ConvertTimeUnitsToString(0));
                        }
                        handler.postDelayed(this,100);
                    }
                },0);
            }
        });
    }

    /**
     * Gets the thumbnail bitmap of the audio.
     * @param audio Audio information.
     * @return Bitmap of the audio.
     */
    public Bitmap GetMediaPlayerIcon(PlaybackAudioInfo audio) {
        Bitmap bitmap = DataManager.getInstance().GetThumbnailImage(audio);
        if(bitmap != null) {
            return bitmap;
        }
        else {
            return BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.med_res);
        }
    }

    /**
     * Converts a time in milliseconds to a formatted string.
     * @param time Time in milliseconds.
     * @return Formatted time as a string.
     */
    public static String ConvertTimeUnitsToString(int time) {
        return String.format(Locale.US,"%02d:%02d",TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time)));
    }

    /**
     * Gets the color code based on the attribute set by the current theme.
     * @param attribute Color attribute defined by the theme.
     * @return Color code based on the attribute.
     */
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

        if(this.musicService != null) {
            //Stops the music service.
            Log.d(LOG_TAG,"Service Unbinding...");
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
}