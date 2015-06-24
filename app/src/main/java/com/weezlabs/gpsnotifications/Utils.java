package com.weezlabs.gpsnotifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Andrey Bondarenko on 25.05.15.
 */
public class Utils {
    private static final double DEFAULT_DOUBLE_VALUE = 0;
    private static final float DEFAULT_FLOAT_VALUE = 0;
    private static final String LAT = "lat_key";
    private static final String LNG = "lng_key";
    private static final String ZOOM = "zoom_key";
    private static final String TILT = "tilt_key";
    private static final String BEARING = "bearing_key";

    /**
     * Prevents instantiation.
     */
    private Utils() {
    }


    public static void saveCameraPosition(Context context, CameraPosition cameraPosition) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        putDouble(editor, LAT, cameraPosition.target.latitude);
        putDouble(editor, LNG, cameraPosition.target.longitude);
        editor.putFloat(ZOOM, cameraPosition.zoom);
        editor.putFloat(TILT, cameraPosition.tilt);
        editor.putFloat(BEARING, cameraPosition.bearing);
        editor.apply();
    }

    public static CameraPosition restoreCameraPosition(Context context) {
        CameraPosition cameraPosition = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        double lat = getDouble(prefs, LAT, DEFAULT_DOUBLE_VALUE);
        double lng = getDouble(prefs, LNG, DEFAULT_DOUBLE_VALUE);
        float zoom = prefs.getFloat(ZOOM, DEFAULT_FLOAT_VALUE);
        float tilt = prefs.getFloat(TILT, DEFAULT_FLOAT_VALUE);
        float bearing = prefs.getFloat(BEARING, DEFAULT_FLOAT_VALUE);
        if (!isNonCorrectValues(lat, lng, zoom, tilt, bearing)) {
            cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lng))
                    .zoom(zoom)
                    .tilt(tilt)
                    .bearing(bearing)
                    .build();
        }
        return cameraPosition;
    }

    private static boolean isNonCorrectValues(double lat, double lng, float zoom, float tilt, float bearing) {
        return lat == DEFAULT_DOUBLE_VALUE && lng == DEFAULT_DOUBLE_VALUE
                && zoom == DEFAULT_FLOAT_VALUE && tilt == DEFAULT_FLOAT_VALUE
                && bearing == DEFAULT_FLOAT_VALUE;
    }

    private static SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    private static double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }
}
