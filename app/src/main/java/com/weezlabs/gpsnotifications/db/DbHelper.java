package com.weezlabs.gpsnotifications.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.weezlabs.gpsnotifications.model.Alarm;

/**
 * Created by Andrey Bondarenko on 22.05.15.
 */
public class DbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "alarms_db";

    private static final String CREATE_ALARM_TABLE = "" +
            "CREATE TABLE " + Alarm.TABLE + "(" +
            Alarm.ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Alarm.LAT + " FLOAT NOT NULL," +
            Alarm.LNG + " FLOAT NOT NULL," +
            Alarm.NAME + " TEXT," +
            Alarm.ADDRESS + " TEXT NOT NULL," +
            Alarm.VIBRATION + " INTEGER NOT NULL," +
            Alarm.SOUND + " INTEGER NOT NULL," +
            Alarm.LED + " INTEGER NOT NULL," +
            Alarm.DISTANCE + " INTEGER NOT NULL" +
            ")";

    private static final String UPGRADE_ALARM_TABLE = "" +
            "DROP TABLE IF EXISTS " + Alarm.TABLE;

    public DbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ALARM_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(UPGRADE_ALARM_TABLE);
        onCreate(db);
    }
}
