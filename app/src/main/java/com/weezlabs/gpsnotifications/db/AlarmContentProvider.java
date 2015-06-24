package com.weezlabs.gpsnotifications.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.weezlabs.gpsnotifications.model.Alarm;

public class AlarmContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.weezlabs.gpsnotifications.provider";

    private static final String SCHEME = "content://";
    public static final Uri BASE_CONTENT_URI = Uri.parse(SCHEME + AUTHORITY);
    private static final String UNKNOWN_URI = "Unknown URI";
    private static final String ALARMS_PATH = "alarms";
    public static final Uri ALARMS_CONTENT_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(ALARMS_PATH).build();
    private static final int ALARMS = 10;
    private static final int ALARM_ID = 11;
    private final static UriMatcher sUriMatcher = buildUriMatcher();

    private DbHelper mDbHelper;

    public AlarmContentProvider() {
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AUTHORITY;

        matcher.addURI(authority, "alarms", ALARMS);
        matcher.addURI(authority, "alarms/#", ALARM_ID);

        return matcher;
    }

    public static Uri buildAlarmIdUri(int alarmId) {
        return ALARMS_CONTENT_URI.buildUpon().appendPath(String.valueOf(alarmId)).build();
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        Uri resultUri;
        long rowId;
        switch (match) {
            case ALARMS:
                rowId = mDbHelper.getWritableDatabase().insert(Alarm.TABLE, null, values);
                resultUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(resultUri, null);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI);
        }

        return resultUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ALARMS:
                count = mDbHelper.getWritableDatabase().update(Alarm.TABLE, values, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case ALARM_ID:
                selection = getAlarmSelection(uri, selection);
                count = mDbHelper.getWritableDatabase().update(Alarm.TABLE, values, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI);
        }

        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        int countRows;
        switch (match) {
            case ALARM_ID:
                selection = getAlarmSelection(uri, selection);
                countRows = mDbHelper.getWritableDatabase().delete(Alarm.TABLE, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            case ALARMS:
                countRows = mDbHelper.getWritableDatabase().delete(Alarm.TABLE, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI);
        }

        return countRows;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        final int match = sUriMatcher.match(uri);
        String groupByString = "";
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (match) {
            case ALARMS:
                queryBuilder.setTables(Alarm.TABLE);
                break;
            case ALARM_ID:
                queryBuilder.appendWhere(Alarm.ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI);
        }

        cursor = queryBuilder.query(mDbHelper.getReadableDatabase(), projection, selection,
                selectionArgs, groupByString, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    private String getAlarmSelection(Uri uri, String selection) {
        String alarmId = uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
            selection = Alarm.ID + "=" + alarmId;
        } else {
            selection = selection + " AND " + Alarm.ID + "=" + alarmId;
        }
        return selection;
    }
}
