package com.example.cloudplaylistmanager.MusicPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "NotificationReceiver";

    public static final String MEDIA_NOTIFICATION_ACTION = "media_notification_action";
    public static final String MEDIA_NOTIFICATION_ACTION_KEY = "media_notification_key";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null) {
            try {
                switch (intent.getAction()) {
                    case ApplicationClass.ACTION_PLAY:
                    case ApplicationClass.ACTION_NEXT:
                    case ApplicationClass.ACTION_PREV:
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
