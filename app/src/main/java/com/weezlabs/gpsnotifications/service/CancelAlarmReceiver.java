package com.weezlabs.gpsnotifications.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.weezlabs.gpsnotifications.model.Alarm;

public class CancelAlarmReceiver extends BroadcastReceiver {

    public static final int REQUEST_CODE = 113;

    public CancelAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(GeofenceIntentService.NOTIFICATION_ID,
                GeofenceIntentService.INCORRECT_VALUE);

        NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);
    }

    public static PendingIntent getCancelIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, CancelAlarmReceiver.class);
        intent.putExtra(GeofenceIntentService.NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
