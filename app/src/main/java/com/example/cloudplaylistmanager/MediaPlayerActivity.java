package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.cloudplaylistmanager.Utils.MusicService;
import com.example.cloudplaylistmanager.Utils.OnUpdatePlayerListener;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.google.android.material.color.MaterialColors;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MediaPlayerActivity extends AppCompatActivity {
    public static final String SERIALIZE_TAG = "data";
    public static final String POSITION_TAG = "position";

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
    private int startingPosition;
    private MusicService musicService = null;
    private ServiceConnection musicServiceConnection;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //Receives the data that was sent to by the calling Activity.
        this.playlistInfo = (PlaylistInfo) getIntent().getSerializableExtra(SERIALIZE_TAG);
        this.startingPosition = getIntent().getIntExtra(POSITION_TAG,0);
        if(this.playlistInfo == null) {
            this.finish();
            return;
        }

        this.handler = new Handler();


        this.musicServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d("MusicService","Service Connected...");
                MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) iBinder;
                musicService = binder.getBinder();
                if(playlistInfo != null) {
                    StartMediaPlayer();
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


        this.mediaPlayerPlaylistTitle.setText(this.playlistInfo.getTitle());


        this.mediaPlayerPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.Pause();
                }
            }
        });

        this.mediaPlayerSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.SwitchSong(MusicService.NEXT_SONG_IGNORED);
                }
            }
        });

        this.mediaPlayerSkipPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicService != null) {
                    musicService.SwitchSong(MusicService.NEXT_SONG_PREV);
                }
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

    }

    public void StartMediaPlayer() {
        ArrayList<PlaybackAudioInfo> allSongs = new ArrayList<>();
        allSongs.addAll(this.playlistInfo.getAllVideos());

        this.musicService.SetOnUpdatePlayerListener(new OnUpdatePlayerListener() {
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
            public void onSongChange(PlaybackAudioInfo audio) {
                mediaPlayerTitle.setText(audio.getTitle());
                if(audio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
                    Bitmap bitmap = BitmapFactory.decodeFile(audio.getThumbnailSource());
                    if (bitmap != null) {
                        mediaPlayerIcon.setImageBitmap(bitmap);
                    }
                    else {
                        mediaPlayerIcon.setImageResource(R.drawable.med_res);
                    }
                }
                else {
                    mediaPlayerIcon.setImageResource(R.drawable.med_res);
                }
            }

            @Override
            public void onPrepared(int duration) {
                mediaPlayerSeekBar.setMax(duration);
                mediaPlayerDurationTime.setText(ConvertTimeUnitsToString(duration));
            }
        });

        this.musicService.InitializePlayer(allSongs);
        this.musicService.BeginPlaying(this.startingPosition,true,true);

        //Updates the Seek Bar
        this.handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (musicService != null) {
                        mediaPlayerSeekBar.setProgress(musicService.GetCurrentPosition());
                        mediaPlayerCurrentTime.setText(ConvertTimeUnitsToString(musicService.GetCurrentPosition()));
                    }
                } catch(Exception e) {
                    mediaPlayerSeekBar.setProgress(0);
                    mediaPlayerCurrentTime.setText(ConvertTimeUnitsToString(0));
                }
                handler.postDelayed(this,100);
            }
        },0);
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

        this.handler.removeCallbacksAndMessages(null);
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
}