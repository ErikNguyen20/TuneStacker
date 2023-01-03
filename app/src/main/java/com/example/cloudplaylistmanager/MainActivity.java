package com.example.cloudplaylistmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.MusicService;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;

    FirebaseAuth authentication = null;

    private ArrayList<PlaybackAudioInfo> playlist;
    String source = "https://c131.pcloud.com/cBZj91AXKZqN9qzGZ7COf7ZZRxKOc7ZlXZZWKVZRZthQYZhLZtVZrpZjLZQ5ZqkZUzZfRZQFZpkZSVZzXZVpZApZpXZ70Ffzwi87SLAnkcdzXUrmHpjGfk7/whitepromise.m4a";
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

        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, this.musicServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(this.authentication.getCurrentUser() != null) {
            //Take user to the landing page.
            //startActivity(new Intent(MainActivity.this,RegisterActivity.class));
        }
        else {
            //Temporary for testing.
            this.authentication.signInWithEmailAndPassword("20nguyened@gmail.com", "Password123");
        }


        DataManager.Initialize(this);
        DataManager manager = DataManager.getInstance();
        if(!CheckPermission()) {
            RequestPermission();
        }

        //manager.UploadAudioToCloud();
        //Uri uri = new Uri("https://c229.pcloud.com/dpZXgjHqwZpDP9P3Z7COf7ZZoasUc7ZlXZZWKVZZ28KpyD2cCzJbbIJ3kcceXYVPpuF7/Kimino%20Shiranai%20Monogatari.mp3");

        //manager.SyncPlaylist("https://www.youtube.com/playlist?list=PLL1BqiG1yrUVtv9Ff2cWxCPW0aIHZYX6o");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(musicService != null) {
            //Stops the music service.
            //unbindService(this.musicServiceConnection);
        }
    }


    /**
     * Checks to see if the user has granted read/write permissions to the app.
     */
    public boolean CheckPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        else {
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests read/write permissions from the user.
     */
    private void RequestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try{
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }
            catch (Exception e) {
                e.printStackTrace();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    private final ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(Environment.isExternalStorageManager()) {
                        Log.d("Permissions", "Storage Permission Granted");
                    }
                    else {
                        Log.d("Permissions", "Storage Permission Denied");
                    }
                }
            });

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE) {
            if(grantResults.length > 0) {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Storage Permission Granted");
                }
                else {
                    Log.d("Permissions", "Storage Permission Denied");
                }
            }
        }
    }

    public void testPlayer() {
        playlist = new ArrayList<>(2);
        PlaybackAudioInfo item = new PlaybackAudioInfo("monogatari",source, PlaybackAudioInfo.PlaybackMediaType.STREAM);
        //PlaybackAudioInfo item2 = new PlaybackAudioInfo("kannagi","unknown",100000,source2, PlaybackAudioInfo.PlaybackMediaType.STREAM);
        playlist.add(item);
        //playlist.add(item2);

        Log.d("MusicPlayer","Initializing...");
        musicService.InitializePlayer(playlist);
        musicService.BeginPlaying(0,true,true);
    }
}