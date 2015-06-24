package com.weezlabs.gpsnotifications.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class CancelIntentService extends IntentService {

    private static final String LOG_TAG = CancelIntentService.class.getSimpleName();

    public CancelIntentService() {
        super("CancelIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            int notificationId = intent.getIntExtra(GeofenceIntentService.NOTIFICATION_ID,
                    GeofenceIntentService.INCORRECT_VALUE);
            NotificationManager manager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
        }
    }

    public static PendingIntent getCancelIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, CancelIntentService.class);
        intent.putExtra(GeofenceIntentService.NOTIFICATION_ID, notificationId);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}
