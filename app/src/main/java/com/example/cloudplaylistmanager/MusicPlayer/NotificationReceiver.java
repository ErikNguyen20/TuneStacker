package com.example.cloudplaylistmanager.MusicPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "NotificationReceiver";

    public static final String MEDIA_NOTIFICATION_ACTION = "media_notification_action";
    public static final String MEDIA_NOTIFICATION_ACTION_KEY = "media_notification_key";
    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_NEXT = "NEXT";
    public static final String ACTION_PREV = "PREVIOUS";
    public static final String ACTION_DELETE = "DELETE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null) {
            try {
                switch (intent.getAction()) {
                    case ACTION_PLAY:
                    case ACTION_NEXT:
                    case ACTION_PREV:
                    case ACTION_PAUSE:
                    case ACTION_DELETE:
                        //Broadcasts the button actions to the media player.
                        Intent local = new Intent();
                        local.setAction(MEDIA_NOTIFICATION_ACTION);
                        local.putExtra(MEDIA_NOTIFICATION_ACTION_KEY, intent.getAction());
                        context.sendBroadcast(local);
                }
            } catch(Exception e) {
                Log.e(LOG_TAG,"Failed to broadcast notification action to MediaPlayerActivity");
                e.printStackTrace();
            }
        }
    }
}
