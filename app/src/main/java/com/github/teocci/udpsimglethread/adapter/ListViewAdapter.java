package com.github.teocci.udpsimglethread.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.teocci.udpsimglethread.R;
import com.github.teocci.udpsimglethread.listeners.ListItemListener;
import com.github.teocci.udpsimglethread.model.DeviceInfo;
import com.github.teocci.udpsimglethread.ui.StateView;
import com.github.teocci.udpsimglethread.utils.LogHelper;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public class ListViewAdapter extends ArrayAdapter<DeviceInfo>
{
    public static final String TAG = LogHelper.makeLogTag(ListViewAdapter.class);

    private ListItemListener listItemListener;
    private final LayoutInflater inflater;
    private final StringBuilder stringBuilder;
    private DeviceInfo[] deviceInfo;

    private static class RowViewInfo
    {
        public final TextView textViewStationName;
        public final TextView textViewAddressAndPing;
        public final TextView textViewUnique;
        public final StateView stateView;

        public RowViewInfo(TextView textViewStationName, TextView textViewAddressAndPing, TextView textViewUnique,
                           StateView stateView)
        {
            this.textViewStationName = textViewStationName;
            this.textViewAddressAndPing = textViewAddressAndPing;
            this.textViewUnique = textViewUnique;
            this.stateView = stateView;
        }
    }

    public ListViewAdapter(Activity context, ListItemListener listItemListener)
    {
        super(context, R.layout.list_view_row);
        this.listItemListener = listItemListener;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        stringBuilder = new StringBuilder();
        deviceInfo = new DeviceInfo[0];
    }

    public void setDeviceInfo(DeviceInfo[] deviceInfo)
    {
        this.deviceInfo = deviceInfo;
        notifyDataSetChanged();
    }

    public int getCount()
    {
        return deviceInfo.length;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View rowView = convertView;
        RowViewInfo rowViewInfo;
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.list_view_row, null, true);
            final TextView textViewStationName = (TextView) rowView.findViewById(R.id.textViewStationName);
            final TextView textViewStationAddress = (TextView) rowView.findViewById(R.id.textViewAddressAndPing);
            final TextView textViewUnique = (TextView) rowView.findViewById(R.id.textViewUnique);
            final StateView stateView = (StateView) rowView.findViewById(R.id.stateView);
            rowViewInfo = new RowViewInfo(textViewStationName, textViewStationAddress, textViewUnique, stateView);
            rowView.setTag(rowViewInfo);
        } else
            rowViewInfo = (RowViewInfo) rowView.getTag();

        final DeviceInfo station = deviceInfo[position];

        rowViewInfo.textViewStationName.setText(station.name);
        rowViewInfo.textViewUnique.setText(station.unique);

        stringBuilder.setLength(0);
        stringBuilder.append(station.address.getHostAddress());
        final long ping = station.ping;
        if (ping > 0) {
            stringBuilder.append(", ");
            stringBuilder.append(station.ping);
            stringBuilder.append(" ms");
        }
        rowViewInfo.textViewAddressAndPing.setText(stringBuilder.toString());
        rowViewInfo.stateView.setIndicatorState(station.transmission);

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listItemListener != null)
                    listItemListener.onListItemClicked(station);
            }
        });

        return rowView;
    }
}