package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.cloudplaylistmanager.Utils.MusicService;

public class MediaPlayerActivity extends AppCompatActivity {

    private ImageView mediaPlayerIcon;
    private TextView mediaPlayerTitle;
    private SeekBar mediaPlayerseekBar;
    private TextView mediaPlayerCurrentTime;
    private TextView mediaPlayerDurationTime;
    private ImageButton mediaPlayerRepeat;
    private ImageButton mediaPlayerSkipPrevious;
    private ImageButton mediaPlayerPlay;
    private ImageButton mediaPlayerSkipNext;
    private ImageButton mediaPlayerShuffle;
    private TextView mediaPlayerPlaylistTitle;

    private MusicService musicService = null;
    private ServiceConnection musicServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setBackgroundDrawable(new ColorDrawable(R.attr.colorPrimaryVariant));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();

        this.musicServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d("MusicService","Service Connected...");
                MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) iBinder;
                musicService = binder.getBinder();
                //testPlayer();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                musicService = null;
                Log.d("MusicService","Service Disconnected");
            }
        };
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, this.musicServiceConnection, Context.BIND_AUTO_CREATE);




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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