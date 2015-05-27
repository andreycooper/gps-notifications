package com.weezlabs.gpsnotifications.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.weezlabs.gpsnotifications.model.Alarm;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class SnoozeIntentService extends IntentService {

    private static final String LOG_TAG = SnoozeIntentService.class.getSimpleName();
    public static final int FIVE_MINUTES = 5 * 60 * 1000;

    public SnoozeIntentService() {
        super("SnoozeIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            int notificationId = intent.getIntExtra(GeofenceIntentService.NOTIFICATION_ID,
                    GeofenceIntentService.INCORRECT_VALUE);
            Alarm alarm = intent.getParcelableExtra(GeofenceIntentService.ALARM_EXTRA);
            Log.d(LOG_TAG, "notification id: " + notificationId);
            Log.d(LOG_TAG, "alarm: " + alarm);
            NotificationManager manager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
            if (alarm != null) {
                snoozeFiveMinutes(alarm);
            }
        }
    }

    public static PendingIntent getSnoozeIntent(Context context, int notificationId, Alarm alarm) {
        Intent snoozeIntent = new Intent(context, SnoozeIntentService.class);
        snoozeIntent.putExtra(GeofenceIntentService.NOTIFICATION_ID, notificationId);
        snoozeIntent.putExtra(GeofenceIntentService.ALARM_EXTRA, alarm);
        return PendingIntent.getService(context, 0, snoozeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void snoozeFiveMinutes(Alarm alarm) {
        try {
            Thread.sleep(FIVE_MINUTES);
        } catch (InterruptedException e) {
            Log.d(LOG_TAG, "Snooze was interrupted");
            e.printStackTrace();
        }
        GeofenceIntentService.sendAlarmNotification(this, alarm);
    }

}
