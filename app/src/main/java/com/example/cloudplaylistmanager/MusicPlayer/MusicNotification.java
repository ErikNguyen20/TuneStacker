package com.example.cloudplaylistmanager.MusicPlayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

public class MusicNotification {
    private static final String LOG_TAG = "MusicNotification";
    private static final String CHANNEL_ID = "com.example.cloudplaylistmanager.MusicPlayer.channel";
    public static final int NOTIFICATION_ID = 512;
    public static final int REQUEST_CODE = 621;
    public static final int ACTION_REQUEST_CODE = 623;

    private MusicBrowserService musicService;
    private NotificationCompat.Action playAction;
    private NotificationCompat.Action pauseAction;
    private NotificationCompat.Action nextAction;
    private NotificationCompat.Action prevAction;
    private NotificationManager notificationManager;

    /**
     * Instantiates a new MusicNotification object
     * @param musicService music service that it is bound to.
     */
    public MusicNotification(MusicBrowserService musicService) {
        this.musicService = musicService;
        this.notificationManager = (NotificationManager) this.musicService.getSystemService(Context.NOTIFICATION_SERVICE);


        //Creates Notification Actions for all of the notification buttons.
        this.playAction = new NotificationCompat.Action(
                R.drawable.ic_baseline_play_circle_outline_24,
                this.musicService.getString(R.string.notification_label_play),
                CreateActionIntent(NotificationReceiver.ACTION_PLAY, PendingIntent.FLAG_UPDATE_CURRENT));

        this.pauseAction = new NotificationCompat.Action(
                R.drawable.ic_baseline_pause_circle_outline_24,
                this.musicService.getString(R.string.notification_label_pause),
                CreateActionIntent(NotificationReceiver.ACTION_PAUSE, PendingIntent.FLAG_UPDATE_CURRENT));

        this.nextAction = new NotificationCompat.Action(
                R.drawable.ic_baseline_skip_next_24,
                this.musicService.getString(R.string.notification_label_next),
                CreateActionIntent(NotificationReceiver.ACTION_NEXT, PendingIntent.FLAG_UPDATE_CURRENT));

        this.prevAction = new NotificationCompat.Action(
                R.drawable.ic_baseline_skip_previous_24,
                this.musicService.getString(R.string.notification_label_prev),
                CreateActionIntent(NotificationReceiver.ACTION_PREV, PendingIntent.FLAG_UPDATE_CURRENT));

        this.notificationManager.cancelAll();
    }

    public void Destroy() {
        this.notificationManager.cancelAll();
    }

    /**
     * Creates a Notification Channel if it does not already exist.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void CreateChannel() {
        if(this.notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            //Name and description of the channel.
            CharSequence name = "MediaPlayerNotification";
            String description = "Notification Channel for MediaPlayer";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);

            this.notificationManager.createNotificationChannel(channel);

            Log.d(LOG_TAG, "New channel created.");
        }
        else {
            Log.d(LOG_TAG, "Channel already exists.");
        }
    }

    /**
     * Builds a notification based on the given data.
     * @param audio Current playing audio data.
     * @param isPlaying If the media player is playing.
     * @param token Current media session token.
     * @return Notification.
     */
    public Notification BuildNotification(PlaybackAudioInfo audio,
                                          boolean isPlaying, MediaSessionCompat.Token token) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CreateChannel();
        }

        //Creates the display on the notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.musicService, CHANNEL_ID);
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0, 1, 2))
                .setSmallIcon(R.drawable.ic_baseline_play_circle_outline_24)
                .setContentTitle(audio.getTitle())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(CreateContentIntent())
                .setLargeIcon(DataManager.getInstance().GetThumbnailImage(audio));

        builder.setDeleteIntent(CreateActionIntent(NotificationReceiver.ACTION_DELETE, PendingIntent.FLAG_CANCEL_CURRENT));

        //Sets button actions.
        builder.addAction(this.prevAction);
        if(isPlaying) {
            builder.addAction(this.pauseAction);
        }
        else {
            builder.addAction(this.playAction);
        }
        builder.addAction(this.nextAction);

        return builder.build();
    }


    /**
     * Creates a pending intent to return to the media player activity.
     * @return New Pending Intent
     */
    private PendingIntent CreateContentIntent() {
        Intent intent = new Intent(this.musicService, MediaPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getActivity(this.musicService, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        }
        else {
            return PendingIntent.getActivity(this.musicService, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    /**
     * Creates a pending intent based on the given ACTION from the NotificationReceiver Class.
     * @param ACTION String ACTION
     * @return New Pending Intent
     */
    private PendingIntent CreateActionIntent(String ACTION, final int flag) {
        Intent intent = new Intent(this.musicService, NotificationReceiver.class).setAction(ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getBroadcast(this.musicService, ACTION_REQUEST_CODE, intent,PendingIntent.FLAG_IMMUTABLE | flag);
        }
        else {
            return PendingIntent.getBroadcast(this.musicService, ACTION_REQUEST_CODE, intent, flag);
        }
    }

    /**
     * Returns the NotificationManager.
     * @return NotificationManager
     */
    public NotificationManager GetNotificationManager() {
        return this.notificationManager;
    }
}
