package com.weezlabs.gpsnotifications.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.weezlabs.gpsnotifications.R;
import com.weezlabs.gpsnotifications.model.Alarm;

import java.util.ArrayList;
import java.util.List;


public class LocationProvider implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<Status> {


    public static final String LOG_TAG = LocationProvider.class.getSimpleName();
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final int SEC = 1000;
    private LocationCallback mLocationCallback;
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;


    public LocationProvider(Context context, LocationCallback callback) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


        mLocationCallback = callback;


        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * SEC)        // 5 seconds, in milliseconds
                .setFastestInterval(2 * SEC); // 1 second, in milliseconds


        mContext = context;
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    public void addGeofenceAlarm(Alarm alarm) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(mContext, mContext.getString(R.string.client_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(alarm),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    public void removeGeofenceAlarm(Alarm alarm) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(mContext, mContext.getString(R.string.client_not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> geofencesRequestIdList = new ArrayList<>();
        geofencesRequestIdList.add(alarm.getGeofenceId());
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    geofencesRequestIdList
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "Location services connected.");

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            mLocationCallback.handleNewLocation(location);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason.
        Log.i(LOG_TAG, "Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution() && mContext instanceof Activity) {
            try {
                Activity activity = (Activity) mContext;
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(activity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            /*
             * Thrown if Google Play services canceled the original
             * PendingIntent
             */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(LOG_TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, "Location received");
        mLocationCallback.handleNewLocation(location);
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            GeofenceStatusCodes.getStatusCodeString(status.getStatusCode());
            Toast.makeText(mContext,
                    "Geofence operation status: "
                            + GeofenceStatusCodes.getStatusCodeString(status.getStatusCode()),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(mContext,
                    status.getStatusCode());
            Log.e(LOG_TAG, errorMessage);
        }
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest(Alarm alarm) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofence to be monitored by geofencing service.
        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(alarm.toGeofence());
        builder.addGeofences(geofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(mContext, GeofenceIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(mContext, 12, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void logSecurityException(SecurityException securityException) {
        Log.e(LOG_TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    public interface LocationCallback {
        void handleNewLocation(Location location);
    }
}