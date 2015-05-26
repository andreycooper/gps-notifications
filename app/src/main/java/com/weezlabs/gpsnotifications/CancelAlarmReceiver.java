package com.weezlabs.gpsnotifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CancelAlarmReceiver extends BroadcastReceiver {

    private static final String NOTIFICATION_ID = "notification_id";
    private static final int INCORRECT_VALUE = -1;

    public CancelAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(NOTIFICATION_ID, INCORRECT_VALUE);
        if (notificationId != INCORRECT_VALUE) {
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
        }
    }

    public static PendingIntent getCancelIntent(Context context, int notificaionId) {
        Intent intent = new Intent(context, CancelAlarmReceiver.class);
        intent.putExtra(NOTIFICATION_ID, notificaionId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
