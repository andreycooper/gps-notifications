package com.weezlabs.gpsnotifications.widget;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.weezlabs.gpsnotifications.R;
import com.weezlabs.gpsnotifications.model.Alarm;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Created by Andrey Bondarenko on 25.05.15.
 */
public class AlarmAdapter extends CursorRecyclerAdapter<AlarmAdapter.ViewHolder> {

    public static final double FEET_RATE = 3.2808;
    public static final String FEET_PATTERN = "0.00";

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;
    private boolean mIsEnLocale;

    public AlarmAdapter(Context context, Cursor cursor) {
        super(cursor);
        mContext = context.getApplicationContext();
        mIsEnLocale = Locale.getDefault().getDisplayLanguage()
                .equals(Locale.ENGLISH.getDisplayLanguage());

    }

    @Override
    public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
        Alarm alarm = new Alarm(cursor);
        if (TextUtils.isEmpty(alarm.getName())) {
            holder.name.setText(mContext.getString(R.string.label_card_name,
                    mContext.getString(R.string.name_alarm_empty)));
        } else {
            holder.name.setText(mContext.getString(R.string.label_card_name, alarm.getName()));
        }
        holder.address.setText(mContext.getString(R.string.label_card_address, alarm.getAddress()));
        holder.vibration.setChecked(alarm.isVibration());
        holder.sound.setChecked(alarm.isSound());
        holder.led.setChecked(alarm.isLed());
        if (mIsEnLocale) {
            double feet = alarm.getDistance() * FEET_RATE;
            DecimalFormat format = new DecimalFormat(FEET_PATTERN);
            holder.distance.setText(mContext.getString(R.string.label_card_distance,
                    format.format(feet), mContext.getString(R.string.label_feet)));
        } else {
            holder.distance.setText(mContext.getString(R.string.label_card_distance,
                    alarm.getDistance(), mContext.getString(R.string.label_meters)));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.card_alarm, viewGroup, false);
        return new ViewHolder(itemView);
    }

    public void setOnItemClickListener(final OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        TextView address;
        CheckBox vibration;
        CheckBox sound;
        CheckBox led;
        TextView distance;

        public ViewHolder(final View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            name = (TextView) itemView.findViewById(R.id.name_text_view);
            address = (TextView) itemView.findViewById(R.id.address_text_view);
            vibration = (CheckBox) itemView.findViewById(R.id.vibration_checkbox);
            sound = (CheckBox) itemView.findViewById(R.id.sound_checkbox);
            led = (CheckBox) itemView.findViewById(R.id.led_checkbox);
            distance = (TextView) itemView.findViewById(R.id.distance_text_view);
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
}
