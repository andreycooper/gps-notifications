package com.weezlabs.gpsnotifications.service;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Andrey Bondarenko on 22.05.15.
 */
public class AddressAsyncLoader extends AsyncTaskLoader<String> {
    public static final String LAT_LNG_KEY = "lat_lng";
    public static final float INCORRECT_VALUE = -1;
    private static final String TAG = AddressAsyncLoader.class.getSimpleName();

    private double mLat;
    private double mLng;

    public AddressAsyncLoader(Context context, Bundle args) {
        super(context);
        if (null != args && args.getParcelable(LAT_LNG_KEY) != null) {
            LatLng latLng = args.getParcelable(LAT_LNG_KEY);
            mLat = latLng.latitude;
            mLng = latLng.longitude;
        } else {
            mLat = INCORRECT_VALUE;
            mLng = INCORRECT_VALUE;
        }
    }

    @Override
    public String loadInBackground() {
        return getAddressString();
    }

    private String getAddressString() {
        String addressStr = null;
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(mLat, mLng, 1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.e(TAG, ioException.getMessage(), ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, illegalArgumentException.getMessage() + ". " +
                    "Latitude = " + mLat +
                    ", Longitude = " + mLng, illegalArgumentException);
        }

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            if (!TextUtils.isEmpty(address.getLocality())
                    && address.getMaxAddressLineIndex() >= 0) {
                // TODO: place more addresses, see yours github
                addressStr = address.getLocality() + ", " + address.getAddressLine(0);
            } else if (!TextUtils.isEmpty(address.getLocality())) {
                addressStr = address.getLocality();
            } else if (address.getMaxAddressLineIndex() >= 0) {
                addressStr = address.getAddressLine(0);
            }
        }

        return addressStr;
    }
}
