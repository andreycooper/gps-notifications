package com.weezlabs.gpsnotifications;

import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.weezlabs.gpsnotifications.db.AlarmContentProvider;
import com.weezlabs.gpsnotifications.model.Alarm;
import com.weezlabs.gpsnotifications.service.AddressAsyncLoader;
import com.weezlabs.gpsnotifications.service.LocationProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements LocationProvider.LocationCallback {
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

    public static final String ALARM_KEY = "com.weezlabs.gpsnotifications.ALARM";

    private static final int REQUEST_CODE = 91;
    private static final int ALARMS_LOADER = 335;
    private static final int ADDRESS_LOADER = 113;
    private static final float ZOOM_LEVEL = 17;
    final private HashMap<Marker, Alarm> mMarkerHashMap = new HashMap<>();
    final private HashMap<Marker, Circle> mCircleHashMap = new HashMap<>();
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LatLng mLatLng;
    private int[] mDistances;
    private LoaderManager.LoaderCallbacks<String> mLoadAddressCallback;
    private LoaderManager.LoaderCallbacks<Cursor> mLoadAlarmsCallback;

    private LocationProvider mLocationProvider;
    private Marker mCurrentPosMarker;
    private CameraPosition mCameraPosition;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        initCallbacks();

        mDistances = getResources().getIntArray(R.array.array_alarm_distances);

        setUpMapIfNeeded();

        mLocationProvider = new LocationProvider(this, this);

        initProgress();

        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

    }

    private void initProgress() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(getString(R.string.toast_progress));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationProvider.connect();
        if (mMap != null) {
            CameraPosition cameraPosition = Utils.restoreCameraPosition(getApplicationContext());
            if (cameraPosition != null) {
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                mCameraPosition = cameraPosition;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMap != null) {
            Utils.saveCameraPosition(getApplicationContext(), mMap.getCameraPosition());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationProvider.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Alarm alarm = data.getParcelableExtra(ALARM_KEY);
            if (alarm != null) {
                LatLng latLng = new LatLng(alarm.getLat(), alarm.getLng());
                mCameraPosition = new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(ZOOM_LEVEL)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition), 1000, null);
            }
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                mLatLng = latLng;
                loadAddress(mLatLng);
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mMarkerHashMap.containsKey(marker)) {
                    showDeleteMarkerDialog(marker);
                } else if (mCurrentPosMarker != null && mCurrentPosMarker.equals(marker)) {
                    mLatLng = marker.getPosition();
                    loadAddress(mLatLng);
                }
                return true;
            }
        });

        loadAlarms();
    }

    private void showDeleteMarkerDialog(final Marker marker) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(MapsActivity.this,
                        R.style.Base_Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle(R.string.title_delete_alarm_dialog);
        builder.setMessage(getString(R.string.label_delete_alarm_dialog,
                mMarkerHashMap.get(marker).getAddress()));
        builder.setCancelable(false);
        builder.setNegativeButton(R.string.label_delete_alarm_dialog_cancel_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setPositiveButton(R.string.label_delete_alarm_dialog_delete_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAlarmAndMarker(marker);
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    private void deleteAlarmAndMarker(Marker marker) {
        Alarm alarm = mMarkerHashMap.remove(marker);
        Circle circle = mCircleHashMap.remove(marker);
        if (alarm.getId() > 0) {
            getContentResolver().delete(AlarmContentProvider.buildAlarmIdUri(alarm.getId()), null, null);
        } else {
            getContentResolver().delete(AlarmContentProvider.ALARMS_CONTENT_URI,
                    Alarm.LAT + "=? AND " + Alarm.LNG + "=?",
                    new String[]{String.valueOf(alarm.getLat()), String.valueOf(alarm.getLng())});
        }
        marker.remove();
        circle.remove();
        mLocationProvider.removeGeofenceAlarm(alarm);
    }

    private void showAddMarkerDialog(final LatLng latLng, final String address) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(MapsActivity.this,
                        R.style.Base_Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle(R.string.title_add_alarm_dialog);
        builder.setCancelable(false);

        View inflatedView = LayoutInflater.from(MapsActivity.this)
                .inflate(R.layout.dialog_add_marker, null);
        final EditText nameEdit = (EditText) inflatedView.findViewById(R.id.name_edit_text);
        final EditText addressEdit = (EditText) inflatedView.findViewById(R.id.address_edit_text);
        final CheckBox vibrationCheck = (CheckBox) inflatedView.findViewById(R.id.vibration_checkbox);
        final CheckBox soundCheck = (CheckBox) inflatedView.findViewById(R.id.sound_checkbox);
        final CheckBox ledCheck = (CheckBox) inflatedView.findViewById(R.id.led_checkbox);
        final TextView distanceLabel = (TextView) inflatedView.findViewById(R.id.distance_label);
        final Spinner distanceSpinner = (Spinner) inflatedView.findViewById(R.id.distance_spinner);

        if (!TextUtils.isEmpty(address)) {
            addressEdit.setText(address);
            addressEdit.setSelection(address.length());
        }
        final ArrayAdapter<String> spinnerAdapter;

        if (Locale.getDefault().getDisplayLanguage().equals(Locale.ENGLISH.getDisplayLanguage())) {
            distanceLabel.setText(getString(R.string.label_add_market_dialog_choose_distance,
                    getString(R.string.label_feet)));
            spinnerAdapter = new ArrayAdapter<>(MapsActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    getResources().getStringArray(R.array.array_distance_feet));
        } else {
            distanceLabel.setText(getString(R.string.label_add_market_dialog_choose_distance,
                    getString(R.string.label_meters)));
            spinnerAdapter = new ArrayAdapter<>(MapsActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    getResources().getStringArray(R.array.array_distance_meters));
        }
        distanceSpinner.setAdapter(spinnerAdapter);

        builder.setView(inflatedView);
        builder.setNegativeButton(R.string.label_add_alarm_dialog_cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.label_add_alarm_dialog_add_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Alarm alarm = new Alarm(latLng.latitude,
                        latLng.longitude,
                        address.trim(),
                        vibrationCheck.isChecked(),
                        soundCheck.isChecked(),
                        ledCheck.isChecked(),
                        mDistances[distanceSpinner.getSelectedItemPosition()]);
                if (!TextUtils.isEmpty(nameEdit.getText().toString())) {
                    alarm.setName(nameEdit.getText().toString().trim());
                }

                getContentResolver().insert(AlarmContentProvider.ALARMS_CONTENT_URI, new Alarm.Builder()
                        .lat(alarm.getLat())
                        .lng(alarm.getLng())
                        .name(alarm.getName())
                        .address(alarm.getAddress())
                        .vibrate(alarm.isVibration())
                        .sound(alarm.isSound())
                        .led(alarm.isLed())
                        .distance(alarm.getDistance())
                        .build());

                mLocationProvider.addGeofenceAlarm(alarm);

                dialog.dismiss();
            }
        });

        mProgressDialog.dismiss();
        builder.create().show();
    }

    private void fillMapMarkers(List<Alarm> alarmList) {
        for (Alarm alarm : alarmList) {
            placeAlarmMarkerOnMap(alarm);
        }
    }

    private void placeAlarmMarkerOnMap(Alarm alarm) {
        if (!mMarkerHashMap.containsValue(alarm)) {
            LatLng latLng = new LatLng(alarm.getLat(), alarm.getLng());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(alarm.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .strokeWidth(getResources().getDimension(R.dimen.circle_stroke_width))
                    .fillColor(getResources().getColor(R.color.circle_color))
                    .strokeColor(Color.TRANSPARENT)
                    .radius(alarm.getDistance())); // In meters

            mMarkerHashMap.put(marker, alarm);
            mCircleHashMap.put(marker, circle);
        }
    }

    private void loadAddress(LatLng latLng) {
        Bundle args = new Bundle();
        args.putParcelable(AddressAsyncLoader.LAT_LNG_KEY, latLng);
        Loader<String> loader = getLoaderManager().getLoader(ADDRESS_LOADER);
        if (loader == null) {
            loader = getLoaderManager().initLoader(ADDRESS_LOADER, args, mLoadAddressCallback);
        } else {
            loader = getLoaderManager().restartLoader(ADDRESS_LOADER, args, mLoadAddressCallback);
        }
        loader.forceLoad();
        mProgressDialog.show();
    }

    private void loadAlarms() {
        Loader<Cursor> loader = getLoaderManager().getLoader(ALARMS_LOADER);
        if (loader == null) {
            loader = getLoaderManager().initLoader(ALARMS_LOADER, null, mLoadAlarmsCallback);
        } else {
            loader = getLoaderManager().restartLoader(ALARMS_LOADER, null, mLoadAlarmsCallback);
        }
        loader.forceLoad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_marker_list) {
            startActivityForResult(new Intent(this, AlarmListActivity.class), REQUEST_CODE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initCallbacks() {
        mLoadAddressCallback = new LoaderManager.LoaderCallbacks<String>() {
            @Override
            public Loader<String> onCreateLoader(int id, Bundle args) {
                switch (id) {
                    case ADDRESS_LOADER:
                        return new AddressAsyncLoader(getApplicationContext(), args);
                    default:
                        return null;
                }
            }

            @Override
            public void onLoadFinished(Loader<String> loader, String data) {
                switch (loader.getId()) {
                    case ADDRESS_LOADER:
                        if (!TextUtils.isEmpty(data)) {
                            showAddMarkerDialog(mLatLng, data);
                        } else {
                            mProgressDialog.dismiss();
                            Toast.makeText(MapsActivity.this, "Can't load address at "
                                    + mLatLng.toString(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onLoaderReset(Loader<String> loader) {
                mProgressDialog.dismiss();
            }
        };

        mLoadAlarmsCallback = new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                switch (id) {
                    case ALARMS_LOADER:
                        return new CursorLoader(getApplicationContext(),
                                AlarmContentProvider.ALARMS_CONTENT_URI,
                                Alarm.ALL_COLUMNS, null, null, null);
                    default:
                        return null;
                }
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                switch (loader.getId()) {
                    case ALARMS_LOADER:
                        List<Alarm> alarmList = new ArrayList<>();
                        if (data != null && data.moveToFirst()) {
                            do {
                                alarmList.add(new Alarm(data));
                            } while (data.moveToNext());
                        }
                        fillMapMarkers(alarmList);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {

            }
        };

    }

    @Override
    public void handleNewLocation(Location location) {
        Log.d(LOG_TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(getString(R.string.title_marker_current_position));
        // remove old marker
        if (mCurrentPosMarker != null) {
            mCurrentPosMarker.remove();
        }

        mCurrentPosMarker = mMap.addMarker(options);
        mCurrentPosMarker.showInfoWindow();
        if (mCameraPosition == null) {
            mCameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(ZOOM_LEVEL)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        }
    }

}
