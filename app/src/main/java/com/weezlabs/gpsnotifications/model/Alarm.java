package com.weezlabs.gpsnotifications.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Andrey Bondarenko on 22.05.15.
 */
public class Alarm implements Parcelable {
    public static final String TABLE = "table";
    public static final String ID = "_id";
    public static final String LAT = "lat";
    public static final String LNG = "lng";
    public static final String NAME = "name";
    public static final String ADDRESS = "address";
    public static final String VIBRATION = "vibro";
    public static final String SOUND = "sound";
    public static final String LED = "led";
    public static final String DISTANCE = "distance";

    private int mId;
    private double mLat;
    private double mLng;
    private String mName;
    private String mAddress;
    private boolean mIsVibration;
    private boolean mIsSound;
    private boolean mIsLed;
    private int mDistance;

    public Alarm(Cursor cursor) {
        mId = cursor.getInt(cursor.getColumnIndex(Alarm.ID));
        mLat = cursor.getDouble(cursor.getColumnIndex(Alarm.LAT));
        mLng = cursor.getDouble(cursor.getColumnIndex(Alarm.LNG));
        mName = cursor.getString(cursor.getColumnIndex(Alarm.NAME));
        mAddress = cursor.getString(cursor.getColumnIndex(Alarm.ADDRESS));
        mIsVibration = (cursor.getInt(cursor.getColumnIndex(Alarm.VIBRATION)) == 1);
        mIsSound = (cursor.getInt(cursor.getColumnIndex(Alarm.SOUND)) == 1);
        mIsLed = (cursor.getInt(cursor.getColumnIndex(Alarm.LED)) == 1);
        mDistance = cursor.getInt(cursor.getColumnIndex(Alarm.DISTANCE));
    }

    public Alarm(double lat, double lng, String address, boolean isVibration, boolean isSound,
                 boolean isLed, int distance) {
        mLat = lat;
        mLng = lng;
        mAddress = address;
        mIsVibration = isVibration;
        mIsSound = isSound;
        mIsLed = isLed;
        mDistance = distance;
    }

    public Alarm(float lat, float lng, String name, String address, boolean isVibration,
                 boolean isSound, boolean isLed, int distance) {
        this(lat, lng, address, isVibration, isSound, isLed, distance);
        mName = name;
    }

    public Alarm(int id, float lat, float lng, String name, String address, boolean isVibration,
                 boolean isSound, boolean isLed, int distance) {
        this(lat, lng, name, address, isVibration, isSound, isLed, distance);
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public double getLat() {
        return mLat;
    }

    public void setLat(double lat) {
        mLat = lat;
    }

    public double getLng() {
        return mLng;
    }

    public void setLng(double lng) {
        mLng = lng;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public boolean isVibration() {
        return mIsVibration;
    }

    public void setIsVibration(boolean isVibration) {
        mIsVibration = isVibration;
    }

    public boolean isSound() {
        return mIsSound;
    }

    public void setIsSound(boolean isSound) {
        mIsSound = isSound;
    }

    public boolean isLed() {
        return mIsLed;
    }

    public void setIsLed(boolean isLed) {
        mIsLed = isLed;
    }

    public int getDistance() {
        return mDistance;
    }

    public void setDistance(int distance) {
        mDistance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alarm alarm = (Alarm) o;

        return Double.compare(alarm.getLat(), getLat()) == 0
                && Double.compare(alarm.getLng(), getLng()) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getLat());
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getLng());
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Alarm{");
        sb.append("mId=").append(mId);
        sb.append(", mLat=").append(mLat);
        sb.append(", mLng=").append(mLng);
        sb.append(", mName='").append(mName).append('\'');
        sb.append(", mAddress='").append(mAddress).append('\'');
        sb.append(", mIsVibration=").append(mIsVibration);
        sb.append(", mIsSound=").append(mIsSound);
        sb.append(", mIsLed=").append(mIsLed);
        sb.append(", mDistance=").append(mDistance);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mId);
        dest.writeDouble(this.mLat);
        dest.writeDouble(this.mLng);
        dest.writeString(this.mName);
        dest.writeString(this.mAddress);
        dest.writeByte(mIsVibration ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsSound ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsLed ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mDistance);
    }

    private Alarm(Parcel in) {
        this.mId = in.readInt();
        this.mLat = in.readDouble();
        this.mLng = in.readDouble();
        this.mName = in.readString();
        this.mAddress = in.readString();
        this.mIsVibration = in.readByte() != 0;
        this.mIsSound = in.readByte() != 0;
        this.mIsLed = in.readByte() != 0;
        this.mDistance = in.readInt();
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel source) {
            return new Alarm(source);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    public static final class Builder {
        private final ContentValues mValues = new ContentValues();

        public Builder id(int id) {
            mValues.put(ID, id);
            return this;
        }

        public Builder lat(float lat) {
            mValues.put(LAT, lat);
            return this;
        }

        public Builder lng(float lng) {
            mValues.put(LNG, lng);
            return this;
        }

        public Builder name(String name) {
            mValues.put(NAME, name);
            return this;
        }

        public Builder address(String address) {
            mValues.put(ADDRESS, address);
            return this;
        }

        public Builder vibrate(boolean isVibrate) {
            mValues.put(VIBRATION, isVibrate ? 1 : 0);
            return this;
        }

        public Builder sound(boolean isSound) {
            mValues.put(SOUND, isSound ? 1 : 0);
            return this;
        }

        public Builder led(boolean isLed) {
            mValues.put(LED, isLed ? 1 : 0);
            return this;
        }

        public Builder distance(int distance) {
            mValues.put(DISTANCE, distance);
            return this;
        }
    }
}
