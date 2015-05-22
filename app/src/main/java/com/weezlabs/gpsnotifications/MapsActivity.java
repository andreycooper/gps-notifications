package com.weezlabs.gpsnotifications;

import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Loader;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.weezlabs.gpsnotifications.model.Alarm;

import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements LocationProvider.LocationCallback,
        LoaderManager.LoaderCallbacks<String> {
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();
    private static final int ADDRESS_LOADER = 113;
    // temp const
    static final LatLng MELBOURNE = new LatLng(-37.813, 144.962);
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LatLng mLatLng;
    final private int[] mDistances = {10, 50, 100, 500};

    private LocationProvider mLocationProvider;
    private Marker mCurrentPosMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        mLocationProvider = new LocationProvider(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mLocationProvider.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationProvider.disconnect();
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
            public void onMapClick(LatLng latLng) {
                mLatLng = latLng;
                loadAddress(mLatLng);
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mCurrentPosMarker != null && mCurrentPosMarker.equals(marker)) {
                    mLatLng = marker.getPosition();
                    loadAddress(mLatLng);
                }
                return true;
            }
        });

        Marker melbourne = mMap.addMarker(new MarkerOptions()
                .position(MELBOURNE)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    }

    private void showAddMarkerDialog(final LatLng latLng, final String address) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(MapsActivity.this,
                        R.style.Base_Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle(R.string.title_dialog_alarm);
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
            distanceLabel.setText(getString(R.string.label_dialog_choose_distance,
                    getString(R.string.label_feet)));
            spinnerAdapter = new ArrayAdapter<>(MapsActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    getResources().getStringArray(R.array.array_distance_feet));
        } else {
            distanceLabel.setText(getString(R.string.label_dialog_choose_distance,
                    getString(R.string.label_meters)));
            spinnerAdapter = new ArrayAdapter<>(MapsActivity.this,
                    android.R.layout.simple_spinner_dropdown_item,
                    getResources().getStringArray(R.array.array_distance_meters));
        }
        distanceSpinner.setAdapter(spinnerAdapter);

        builder.setView(inflatedView);
        builder.setNegativeButton(R.string.label_dialog_cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.label_dialog_add_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MapsActivity.this, "LatLng: " + latLng.toString() + "\n"
                        + "Address: " + address, Toast.LENGTH_SHORT).show();
                // TODO: check fields, create alarm, save it to db and place marker with circle
                Alarm alarm = new Alarm(latLng.latitude,
                        latLng.longitude,
                        address,
                        vibrationCheck.isChecked(),
                        soundCheck.isChecked(),
                        ledCheck.isChecked(),
                        mDistances[distanceSpinner.getSelectedItemPosition()]);
                if (!TextUtils.isEmpty(nameEdit.getText().toString())) {
                    alarm.setName(nameEdit.getText().toString());
                }

                placeAlarmMarkerOnMap(alarm);
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private void placeAlarmMarkerOnMap(Alarm alarm) {
        // TODO: place alarm

    }

    private void loadAddress(LatLng latLng) {
        // TODO: maybe show progress
        Bundle args = new Bundle();
        args.putParcelable(AddressAsyncLoader.LAT_LNG_KEY, latLng);
        Loader<String> loader = getLoaderManager().getLoader(ADDRESS_LOADER);
        if (loader == null) {
            loader = getLoaderManager().initLoader(ADDRESS_LOADER, args, this);
        } else {
            loader = getLoaderManager().restartLoader(ADDRESS_LOADER, args, this);
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
            Toast.makeText(this, "TODO: implement marker's list", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        mCurrentPosMarker = mMap.addMarker(options);
        mCurrentPosMarker.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

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
                showAddMarkerDialog(mLatLng, data);
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

}
