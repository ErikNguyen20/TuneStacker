package com.example.cloudplaylistmanager.MusicPlayer;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;


public class MusicBrowserService extends Service {
    private static final String LOG_TAG = "MusicBrowserService";

    private OnUpdatePlayerListener clientCallback;
    private boolean serviceIsStarted;

    private MusicBrowserService.MusicServiceBinder musicBinder = new MusicBrowserService.MusicServiceBinder();
    private MediaSessionCompat session;
    private MusicPlayerCallback callback;
    private MusicPlayback playback;

    private MusicNotification musicNotificationManager;
    private BroadcastReceiver notificationReceiver;
    private PlaybackStateCompat.Builder playbackStateBuilder;


    /**
     * Initializes and Starts the music player.
     * Also instantiates a new Playback object.
     * @param playlist Playlist that will be played.
     * @param startPos Starting position.
     * @param shuffle If the play is shuffled.
     * @param repeat If the play is repeated.
     * @param listener Listener for state of player.
     */
    public void StartPlayer(PlaylistInfo playlist,
                                 int startPos, boolean shuffle, boolean repeat,
                                 OnUpdatePlayerListener listener) {
        this.clientCallback = listener;

        this.playback = new MusicPlayback(this, playlist.getAllVideos(), this.callback);
        this.playback.BeginPlaying(startPos, shuffle, repeat);
    }

    /**
     * Manages Client Control for the play button.
     */
    public void ClientControlPlayButton() {
        if(this.playback == null) {
            return;
        }

        if(this.playback.IsPaused()) {
            this.callback.onPlay();
        }
        else {
            this.callback.onPause();
        }
    }

    /**
     * Manages Client Control for the seek bar.
     */
    public void ClientControlSeekTo(int pos) {
        this.callback.onSeekTo(pos);
    }

    /**
     * Manages Client Control for the repeat button.
     */
    public void ClientControlRepeat() {
        if(this.playback != null) {
            this.playback.SetRepeat(!this.playback.IsRepeat());
        }
    }

    /**
     * Manages Client Control for the shuffle button.
     */
    public void ClientControlShuffle() {
        if(this.playback != null) {
            this.playback.SetShuffle(!this.playback.IsShuffle());
        }
    }

    /**
     * Manages Client Control for changing songs.
     */
    public void ClientControlSwitchSong(int pos) {
        if(this.playback != null) {
            this.playback.SwitchSong(pos);
        }
    }

    /**
     * Retrieves the current position of the playing song.
     */
    public int GetCurrentPosition() {
        if(this.playback != null) {
            return this.playback.GetCurrentPosition();
        }
        else {
            return 0;
        }
    }

    /**
     * Retrieves the current song's index.
     */
    public int GetCurrentSongIndex() {
        if(this.playback != null) {
            return this.playback.GetCurrentSongIndex();
        }
        else {
            return -1;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.musicBinder;
    }
    public class MusicServiceBinder extends Binder {
        public MusicBrowserService getBinder() {
            return MusicBrowserService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.musicNotificationManager = new MusicNotification(this);
        this.callback = new MusicPlayerCallback();

        //Set session
        this.session = new MediaSessionCompat(this, LOG_TAG);
        this.session.setCallback(this.callback);
        this.session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Sets initial playback state
        this.playbackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);

        this.session.setPlaybackState(playbackStateBuilder.build());

        //Sets up the notification media buttons.
        this.notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent == null) {
                    return;
                }
                Log.e(LOG_TAG,"Action Called");
                switch(intent.getStringExtra(NotificationReceiver.MEDIA_NOTIFICATION_ACTION_KEY)) {
                    case NotificationReceiver.ACTION_PLAY:
                        callback.onPlay();
                        break;
                    case NotificationReceiver.ACTION_PAUSE:
                        callback.onPause();
                        break;
                    case NotificationReceiver.ACTION_NEXT:
                        callback.onSkipToNext();
                        break;
                    case NotificationReceiver.ACTION_PREV:
                        callback.onSkipToPrevious();
                        break;
                    case NotificationReceiver.ACTION_DELETE:
                        Log.e(LOG_TAG,"Action Delete Called");
                        stopSelf();
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationReceiver.MEDIA_NOTIFICATION_ACTION);
        registerReceiver(this.notificationReceiver, filter);
    }


    @Override
    public void onDestroy() {
        StopService();
        if(this.musicNotificationManager != null) {
            this.musicNotificationManager.Destroy();
            this.musicNotificationManager = null;
        }
        if(session.isActive()) {
            session.setActive(false);
        }
        this.session.release();
        if(this.playback != null) {
            this.playback.Destroy();
            this.playback = null;
        }
        if(this.notificationReceiver != null) {
            unregisterReceiver(this.notificationReceiver);
            this.notificationReceiver = null;
        }

        Log.d(LOG_TAG, "Destroyed BrowserService");
    }

    /**
     * Starts the media browser service as a foreground service.
     */
    protected void StartServiceForeground() {
        Notification notification = this.musicNotificationManager.BuildNotification(
                this.playback.GetAudioInfo(),
                this.playback.IsPlaying(), this.session.getSessionToken());
        if(!this.serviceIsStarted) {
            //Starts the service
            ContextCompat.startForegroundService(MusicBrowserService.this,
                    new Intent(MusicBrowserService.this, MusicBrowserService.class));
            this.serviceIsStarted = true;
        }

        startForeground(MusicNotification.NOTIFICATION_ID, notification);
    }

    /**
     * Updates the media browser foreground service.
     */
    protected void UpdateNotificationInForeground() {
        if(this.playback == null) {
            return;
        }

        if(this.playback.IsPaused() && this.serviceIsStarted) {
            //If it is paused, stop the foreground service temporarily.
            stopForeground(false);
            Notification notification = this.musicNotificationManager.BuildNotification(
                    this.playback.GetAudioInfo(),
                    this.playback.IsPlaying(), this.session.getSessionToken());

            this.musicNotificationManager.GetNotificationManager().notify(MusicNotification.NOTIFICATION_ID, notification);
        }
        else {
            //Starts the foreground service.
            StartServiceForeground();
        }
    }

    /**
     * Stops the foreground service.
     */
    public void StopService() {
        this.serviceIsStarted = false;
        stopForeground(true);
    }

    public class MusicPlayerCallback extends MediaSessionCompat.Callback implements OnUpdatePlayerListener {

        /**
         * Updates the notification with the most recent playback state.
         */
        public void onUpdatePlaybackState() {
            if(playback == null) {
                return;
            }

            if(playback.IsPlaying()) {
                session.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, playback.GetCurrentPosition(), 1.0F).build());
            }
            else {
                session.setPlaybackState(new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, playback.GetCurrentPosition(), 0.0F).build());
            }
        }

        //Callbacks from the media notification buttons.
        @Override
        public void onPrepare() {
            if(playback == null) {
                return;
            }

            session.setMetadata(new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, playback.GetAudioInfo().getTitle())
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                DataManager.getInstance().GetAudioDuration(playback.GetAudioInfo()))
                        .build());

            onUpdatePlaybackState();

            if(!session.isActive()) {
                session.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            if(playback != null) {
                onPrepare();
                playback.Play();
            }
        }

        @Override
        public void onPause() {
            if(playback != null) {
                playback.Pause();
            }
        }

        @Override
        public void onStop() {
            if(playback != null) {
                playback.Stop();
            }
            session.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            if(playback != null) {
                playback.SwitchSong(MusicPlayback.NEXT_SONG_IGNORED);
            }
        }

        @Override
        public void onSkipToPrevious() {
            if(playback != null) {
                playback.SwitchSong(MusicPlayback.NEXT_SONG_PREV);
            }
        }

        @Override
        public void onSeekTo(long pos) {
            if(playback != null) {
                playback.SeekTo((int) pos);
            }
        }


        //MediaPlayback update callbacks
        @Override
        public void onPauseUpdate(boolean isPaused) {
            clientCallback.onPauseUpdate(isPaused);
            UpdateNotificationInForeground();
            onUpdatePlaybackState();
        }

        @Override
        public void onSeekChange(int pos) {
            onUpdatePlaybackState();
        }

        @Override
        public void onShuffleUpdate(boolean isShuffled) {
            clientCallback.onShuffleUpdate(isShuffled);
        }

        @Override
        public void onRepeatUpdate(boolean isRepeat) {
            clientCallback.onRepeatUpdate(isRepeat);
        }

        @Override
        public void onSongChange(PlaybackAudioInfo audio, int position) {
            onPrepare();
            clientCallback.onSongChange(audio, position);
            UpdateNotificationInForeground();
        }

        @Override
        public void onPrepared(int duration) {
            clientCallback.onPrepared(duration);
        }

        @Override
        public void onEnd() {
            clientCallback.onEnd();
        }
    }
}
