package com.example.cloudplaylistmanager.Utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;


/**
 * Preforms download operations on a service.
 * Extends {@link Service}.
 */
public class DownloadService extends Service {
    private static final String LOG_TAG = "DownloadService";
    private static final String WAKE_LOCK_TAG = "popup:single";
    private static final String WIFI_LOCK_TAG = "popup_download";

    private DownloadServiceBinder downloadServiceBinder = new DownloadServiceBinder();
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;


    /**
     * Downloads a given playlist using the PlatformCompatUtility.
     * @param urlInput URL link to playlist
     * @param playlistListener Listener to retrieve events.
     */
    public void StartPlaylistDownload(String urlInput, DownloadPlaylistListener playlistListener) {
        //Starts wake and wifi lock.
        if(this.wakeLock != null && !this.wakeLock.isHeld()) {
            this.wakeLock.acquire(120*60*1000L /*120 minutes*/);
        }
        if(this.wifiLock != null && !this.wifiLock.isHeld()) {
            this.wifiLock.acquire();
        }

        //Downloads the playlist.
        Log.d(LOG_TAG,"Downloading Playlist.");
        PlatformCompatUtility.DownloadPlaylist(urlInput, new DownloadPlaylistListener() {
            @Override
            public void onComplete(PlaylistInfo playlist) {
                playlistListener.onComplete(playlist);
                ReleaseLocks();
            }

            @Override
            public void onProgressUpdate(String message) {
                playlistListener.onProgressUpdate(message);
            }

            @Override
            public void onError(int attempt, String error) {
                playlistListener.onError(attempt, error);
                if(attempt == -1) {
                    ReleaseLocks();
                }
            }
        });
    }

    /**
     * Downloads a given song using the PlatformCompatUtility.
     * @param urlInput URL link to playlist
     * @param audioListener Listener to retrieve events.
     */
    public void StartAudioDownload(String urlInput, DownloadListener audioListener) {
        //Starts wake and wifi lock.
        if(this.wakeLock != null && !this.wakeLock.isHeld()) {
            this.wakeLock.acquire(10*60*1000L /*10 minutes*/);
        }
        if(this.wifiLock != null && !this.wifiLock.isHeld()) {
            this.wifiLock.acquire();
        }

        //Downloads the song.
        Log.d(LOG_TAG,"Downloading Audio.");
        PlatformCompatUtility.DownloadSong(urlInput, new DownloadListener() {
            @Override
            public void onComplete(PlaybackAudioInfo audio) {
                audioListener.onComplete(audio);
                ReleaseLocks();
            }

            @Override
            public void onProgressUpdate(float progress, long etaSeconds) {
                audioListener.onProgressUpdate(progress, etaSeconds);
            }

            @Override
            public void onError(int attempt, String error) {
                audioListener.onError(attempt, error);
                if(attempt == -1) {
                    ReleaseLocks();
                }
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG,"Created");
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,WAKE_LOCK_TAG);
        this.wifiLock = ((WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL,WIFI_LOCK_TAG);
    }

    @Override
    public void onDestroy() {
        ReleaseLocks();
        Log.d(LOG_TAG,"Destructor");
        super.onDestroy();
    }

    /**
     * Release the wake and wifi lock.
     */
    private void ReleaseLocks() {
        if(this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        if(this.wifiLock != null && this.wifiLock.isHeld()) {
            this.wifiLock.release();
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.downloadServiceBinder;
    }
    public class DownloadServiceBinder extends Binder {
        public DownloadService getBinder() {
            return DownloadService.this;
        }
    }
}
