package com.weezlabs.gpsnotifications.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.weezlabs.gpsnotifications.MapsActivity;
import com.weezlabs.gpsnotifications.R;
import com.weezlabs.gpsnotifications.db.AlarmContentProvider;
import com.weezlabs.gpsnotifications.model.Alarm;

import java.util.ArrayList;
import java.util.List;


public class GeofenceIntentService extends IntentService {
    protected static final String LOG_TAG = GeofenceIntentService.class.getSimpleName();
    public static final int REQUEST_CODE = 11;
    public static final String ALARM_EXTRA = "com.weezlabs.gpsnotifications.ALARM";
    public static final String NOTIFICATION_ID = "com.weezlabs.gpsnotifications.NOTIFICATION_ID";
    public static final int INCORRECT_VALUE = -1;

    public GeofenceIntentService() {
        super("GeofenceIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(LOG_TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // TODO: get Alarms for geofences using ContentProvider
            List<Alarm> triggeringAlarms = getTriggeringAlarmList(triggeringGeofences);
            for (Alarm alarm : triggeringAlarms) {
                Log.d(LOG_TAG, alarm.toString());
                sendAlarmNotification(this, alarm);
            }


            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    geofenceTransition,
                    triggeringGeofences
            );

            Log.i(LOG_TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(LOG_TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    private List<Alarm> getTriggeringAlarmList(List<Geofence> triggeringGeofences) {
        List<Alarm> alarmList = new ArrayList<>();
        Cursor cursor;
        Alarm alarm;
        for (Geofence geofence : triggeringGeofences) {
            String[] latLng = geofence.getRequestId().split(Alarm.LAT_LNG_DELIMITER);
            cursor = getContentResolver().query(AlarmContentProvider.ALARMS_CONTENT_URI, Alarm.ALL_COLUMNS
                    , Alarm.LAT + "=? AND " + Alarm.LNG + "=?", latLng, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    alarm = new Alarm(cursor);
                    alarmList.add(alarm);
                } while (cursor.moveToNext());
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return alarmList;
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition  The ID of the geofence transition.
     * @param triggeringGeofences The geofence(s) triggered.
     * @return The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.geofence_unknown_transition);
        }
    }

    public static void sendAlarmNotification(Context context, Alarm alarm) {
        int notificationId = alarm.getId();

        Log.d(LOG_TAG, "notification id: " + notificationId);
        Log.d(LOG_TAG, "alarm: " + alarm);

        Intent resultIntent = new Intent(context, MapsActivity.class);
        resultIntent.putExtra(ALARM_EXTRA, alarm);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, REQUEST_CODE,
                resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent cancelPendingIntent = CancelIntentService.getCancelIntent(context, notificationId);
        PendingIntent snoozePendingIntent = SnoozeIntentService.getSnoozeIntent(context, notificationId, alarm);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getApplicationContext().getResources(),
                R.drawable.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notif_small)
                .setLargeIcon(largeIcon)
                .setContentTitle(context.getString(R.string.geofence_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.geofence_notification_text, alarm.getAddress())))
                .setContentIntent(resultPendingIntent)
                .addAction(R.drawable.ic_action_cancel,
                        context.getString(R.string.geofence_notification_cancel), cancelPendingIntent)
                .addAction(R.drawable.ic_action_repeat,
                        context.getString(R.string.geofence_notification_repeat), snoozePendingIntent);

        if (alarm.isSound()) {
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        }
        if (alarm.isVibration()) {
            long[] starWarsPattern =
                    new long[]{0, 500, 110, 500, 110, 450, 110, 200, 110, 170, 40, 450, 110, 200, 110, 170, 40, 500};
            builder.setVibrate(starWarsPattern);
        }
        if (alarm.isLed()) {
            builder.setLights(Color.GREEN, 3000, 3000);
        }

        builder.setAutoCancel(false);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }

}
