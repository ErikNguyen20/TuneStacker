package com.example.cloudplaylistmanager.Utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
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

    public static final String INTENT_URL_TAG = "download_url";
    public static final String INTENT_PLAYLIST_TAG = "is_playlist";
    public static final String INTENT_PARENT_UUID_TAG = "uuid_tag_parent";

    public static final String BROADCAST_ACTION_IDENTIFIER = "download_broadcast_action_identifier";
    public static final String BROADCAST_NOTIFICATION_COMPLETE = "download_broadcast_complete";
    public static final String BROADCAST_NOTIFICATION_PROGRESS_MESSAGE = "download_broadcast_progress_message";
    public static final String BROADCAST_NOTIFICATION_ERROR = "download_broadcast_error";
    public static final String BROADCAST_NOTIFICATION_ERROR_CODE = "download_broadcast_error_code";
    public static final String BROADCAST_NOTIFICATION_ERROR_MESSAGE = "download_broadcast_error_message";


    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;


    /**
     * It is crucial to pass extras through the intent when starting this service.
     * INTENT_URL_TAG must be defined as the download url.
     * INTENT_PARENT_UUID_TAG must be defined as the parent uuid of the playlist.
     * INTENT_PLAYLIST_TAG must be defined as if the download parameter is of a playlist or not.
     * @param intent Intent that is being passed.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra(INTENT_URL_TAG);
        String parentUUID = intent.getStringExtra(INTENT_PARENT_UUID_TAG);
        boolean isPlaylist = intent.getBooleanExtra(INTENT_PLAYLIST_TAG,false);
        if(isPlaylist) {
            StartPlaylistDownload(url, parentUUID);
        }
        else {
            StartAudioDownload(url, parentUUID);
        }
        return START_STICKY;
    }

    /**
     * Downloads a given playlist using the PlatformCompatUtility.
     * @param urlInput URL link to playlist
     */
    public void StartPlaylistDownload(String urlInput, String uuid) {
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
                //When the playlist is successfully downloaded, add the playlist to DataManager.
                if(uuid != null && !uuid.isEmpty()) {
                    DataManager.getInstance().CreateNewPlaylist(playlist,false,uuid);
                }
                else {
                    DataManager.getInstance().CreateNewPlaylist(playlist,false,null);
                }

                BroadcastComplete();
                stopSelf();
            }

            @Override
            public void onProgressUpdate(String message) {
                BroadcastProgress(message);
            }

            @Override
            public void onError(int attempt, String error) {
                BroadcastError(attempt, error);
                if(attempt == -1) {
                    stopSelf();
                }
            }
        });
    }

    /**
     * Downloads a given song using the PlatformCompatUtility.
     * @param urlInput URL link to playlist
     */
    public void StartAudioDownload(String urlInput, String uuid) {
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
                //Add the audio to the playlist.
                if(uuid != null && !uuid.isEmpty()) {
                    DataManager.getInstance().AddSongToPlaylist(uuid,audio);
                }

                BroadcastComplete();
                stopSelf();
            }

            @Override
            public void onProgressUpdate(float progress, long etaSeconds) {
                BroadcastProgress("Download Progress: " + progress);
            }

            @Override
            public void onError(int attempt, String error) {
                BroadcastError(attempt, error);
                if(attempt == -1) {
                    stopSelf();
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
        return null;
    }

    /**
     * Sends a Complete broadcast that updates the UI.
     */
    private void BroadcastComplete() {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION_IDENTIFIER);
        intent.putExtra(BROADCAST_ACTION_IDENTIFIER, BROADCAST_NOTIFICATION_COMPLETE);

        sendBroadcast(intent);
    }

    /**
     * Sends a Progress broadcast that updates the UI.
     * @param message Progress Message.
     */
    private void BroadcastProgress(String message) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION_IDENTIFIER);
        intent.putExtra(BROADCAST_ACTION_IDENTIFIER, BROADCAST_NOTIFICATION_PROGRESS_MESSAGE);
        intent.putExtra(BROADCAST_NOTIFICATION_PROGRESS_MESSAGE, message);

        sendBroadcast(intent);
    }

    /**
     * Sends a Error broadcast that updates the UI.
     * @param errorCode Error code of the download.
     * @param message Error message.
     */
    private void BroadcastError(int errorCode, String message) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION_IDENTIFIER);
        intent.putExtra(BROADCAST_ACTION_IDENTIFIER, BROADCAST_NOTIFICATION_ERROR);
        intent.putExtra(BROADCAST_NOTIFICATION_ERROR_CODE, errorCode);
        intent.putExtra(BROADCAST_NOTIFICATION_ERROR_MESSAGE,message);

        sendBroadcast(intent);
    }
}
