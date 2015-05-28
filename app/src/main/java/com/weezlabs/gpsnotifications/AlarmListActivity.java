package com.weezlabs.gpsnotifications;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.weezlabs.gpsnotifications.db.AlarmContentProvider;
import com.weezlabs.gpsnotifications.model.Alarm;
import com.weezlabs.gpsnotifications.widget.AlarmAdapter;


public class AlarmListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ALARMS_LOADER = 287;

    private AlarmAdapter mAlarmAdapter;
    private RecyclerView mAlarmListRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        initList();
        mAlarmAdapter = new AlarmAdapter(this, null);
        mAlarmListRecyclerView.setAdapter(mAlarmAdapter);
        mAlarmAdapter.setOnItemClickListener(new AlarmAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Alarm alarm = null;
                Cursor cursor = mAlarmAdapter.getCursor();
                if (cursor != null) {
                    cursor.moveToPosition(position);
                    alarm = new Alarm(cursor);
                }
                if (alarm != null) {
                    Intent intent = new Intent();
                    intent.putExtra(MapsActivity.ALARM_KEY, alarm);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        loadAlarms();
    }

    private void initList() {
        mAlarmListRecyclerView = (RecyclerView) findViewById(R.id.alarm_list);
        mAlarmListRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mAlarmListRecyclerView.setLayoutManager(layoutManager);
    }

    private void loadAlarms() {
        Loader<Cursor> loader = getLoaderManager().getLoader(ALARMS_LOADER);
        if (loader == null) {
            loader = getLoaderManager().initLoader(ALARMS_LOADER, null, this);
        } else {
            loader = getLoaderManager().restartLoader(ALARMS_LOADER, null, this);
        }
        loader.forceLoad();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
                mAlarmAdapter.changeCursor(data);
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case ALARMS_LOADER:
                mAlarmAdapter.changeCursor(null);
                break;
            default:
                break;
        }
    }
}
