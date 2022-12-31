package com.example.cloudplaylistmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth authentication = null;

    private ArrayList<PlaybackAudioInfo> playlist;
    String source = "https://c166.pcloud.com/dpZXgjHqwZpDP9P3Z7COf7ZZJxQOc7ZlXZZWKVZZsq811h9pXE7vR3KqqU4K9VE7k3KX/Kimino%20Shiranai%20Monogatari.mp3";
    String source2 = "https://c402.pcloud.com/dpZeUWHqwZr0Dtg3Z7COf7ZZE9mOc7ZlXZZWKVZZm51CA2v5CpQ7F43D7Jnc0RxcDqfX/Kannagi%20Opening%20Lyrics%20%28KanjiRomanjiT%C3%BCrk%C3%A7e%29.mp3";

    MusicService musicService = null;
    ServiceConnection musicServiceConnection = new ServiceConnection() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.authentication = FirebaseAuth.getInstance();
        if(this.authentication.getCurrentUser() != null) {
            //Take user to the landing page.
            //startActivity(new Intent(MainActivity.this,RegisterActivity.class));
        }

        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, this.musicServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();


        //startActivity(new Intent(MainActivity.this,RegisterActivity.class));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(musicService != null) {
            //Stops the music service.
            //unbindService(this.musicServiceConnection);
        }
    }

    public void testPlayer() {
        playlist = new ArrayList<>(2);
        PlaybackAudioInfo item = new PlaybackAudioInfo("monogatari","unknown",100000,source, PlaybackAudioInfo.PlaybackMediaType.STREAM);
        PlaybackAudioInfo item2 = new PlaybackAudioInfo("kannagi","unknown",100000,source2, PlaybackAudioInfo.PlaybackMediaType.STREAM);
        playlist.add(item);
        playlist.add(item2);

        Log.d("MusicPlayer","Initializing...");
        musicService.InitializePlayer(playlist);
        musicService.BeginPlaying(0,true,true);
    }
}