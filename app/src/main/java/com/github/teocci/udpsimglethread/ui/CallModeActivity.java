package com.github.teocci.udpsimglethread.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.teocci.udpsimglethread.DeviceManager;
import com.github.teocci.udpsimglethread.R;
import com.github.teocci.udpsimglethread.adapter.ListViewAdapter;
import com.github.teocci.udpsimglethread.listeners.ContactUpdateReceiver;
import com.github.teocci.udpsimglethread.listeners.ListItemListener;
import com.github.teocci.udpsimglethread.model.DeviceInfo;
import com.github.teocci.udpsimglethread.utils.Common;
import com.github.teocci.udpsimglethread.utils.Config;
import com.github.teocci.udpsimglethread.utils.LogHelper;
import com.github.teocci.udpsimglethread.utils.Networking;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public class CallModeActivity extends Activity implements ContactUpdateReceiver, ListItemListener
{
    public static final String TAG = LogHelper.makeLogTag(CallModeActivity.class);

    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;

    private CallModeActivity context;
    private DeviceManager deviceManager;

    private String deviceName;
    private String deviceUnique;
    private List<String> deviceNameList;

    private ListViewAdapter listViewAdapter;

    private boolean isExit;

    private boolean STARTED = false;
    private boolean IN_CALL = false;
    private boolean LISTEN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_mode);
        context = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initiates the main functionality
        LogHelper.e(TAG, "Start button pressed");
        STARTED = true;

        listViewAdapter = new ListViewAdapter(this, this);
        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(listViewAdapter);

        final TextView textView = (TextView) findViewById(R.id.textViewStatus);
        textView.setTextColor(Color.GREEN);

        TextView text = (TextView) findViewById(R.id.textViewSelectContact);
        text.setVisibility(View.VISIBLE);
    }

    public void onResume()
    {
        super.onResume();
        LogHelper.i(TAG, "onResume");

        isExit = false;
        listViewAdapter.clear();

        final SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        deviceName = sharedPreferences.getString(Config.KEY_DEVICE_NAME, null);
        if ((deviceName == null) || deviceName.isEmpty())
            deviceName = Common.getDeviceID(getContentResolver());

        Set<String> set = sharedPreferences.getStringSet(Config.KEY_DEVICE_NAME_LIST, null);
        if (set == null) {
            deviceNameList = new ArrayList<>();
        } else {
            deviceNameList = new ArrayList<>(set);
        }

        if (!deviceName.isEmpty()) {
            final String title = getString(R.string.app_name) + ": " + deviceName;
            setTitle(title);
        }

        deviceManager = new DeviceManager(deviceName, Networking.getIPAddress(true), Networking.getBroadcastIp());
        deviceManager.setContactUpdateReceiver(context);
        startCallListener();

        /** Check WiFi status important!*/
        final boolean checkWiFiStatus = sharedPreferences.getBoolean(Config.KEY_CHECK_WIFI_STATUS, true);
        if (checkWiFiStatus) {
            final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (!manager.isWifiEnabled()) {
                final LayoutInflater layoutInflater = LayoutInflater.from(this);
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                final View dialogView = layoutInflater.inflate(R.layout.dialog_wifi, null);
                final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        LogHelper.d(TAG, "WiFiDialog: which=" + which);
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            manager.setWifiEnabled(true);
                        }
                        final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.checkBoxNeverAskAgain);
                        if (checkBox.isChecked()) {
                            final SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(Config.KEY_CHECK_WIFI_STATUS, false);
                            editor.apply();
                        }
                    }
                };
                dialogBuilder.setView(dialogView);
                dialogBuilder.setPositiveButton(getString(R.string.turn_wifi_on), listener);
                dialogBuilder.setNegativeButton(getString(R.string.cancel), listener);
                final AlertDialog dialog = dialogBuilder.create();
                dialog.show();
            }
        }
    }

    private void updateContactList()
    {
        // Creates a copy of the HashMap used by the DeviceManager
        HashMap<String, InetAddress> contacts = deviceManager.getContacts();
        // Creates a radio button for each contact in the HashMap
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.contactList);
        radioGroup.removeAllViews();

        for (String name : contacts.keySet()) {

            RadioButton radioButton = new RadioButton(getBaseContext());
            radioButton.setText(name);
            radioButton.setTextColor(Color.BLACK);
            radioGroup.addView(radioButton);
        }

        radioGroup.clearCheck();
    }

    private InetAddress getBroadcastIp()
    {
        // Function to return the broadcast address, based on the IP address of the device
        try {

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            String addressString = toBroadcastIp(ipAddress);
            InetAddress broadcastAddress = InetAddress.getByName(addressString);
            return broadcastAddress;
        } catch (UnknownHostException e) {

            LogHelper.e(TAG, "UnknownHostException in getBroadcastIP: " + e);
            return null;
        }

    }

    private String toBroadcastIp(int ip)
    {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }

    private void startCallListener()
    {
        // Creates the listener thread
        LISTEN = true;
        Thread listener = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try {
                    // Sets up the socket and packet to receive
                    LogHelper.e(TAG, "Incoming call listener started");
                    DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
                    socket.setSoTimeout(1000);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while (LISTEN) {
                        // Listens for incoming call requests
                        try {
                            LogHelper.e(TAG, "Listening for incoming calls");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            LogHelper.e(TAG, "Packet received from " + packet.getAddress() + " with contents: " + data);
                            String action = data.substring(0, 4);
                            if (action.equals("CAL:")) {
                                // Starts the ReceiveCallActivity if receives a call request.
                                String address = packet.getAddress().toString();
                                String name = data.substring(4, packet.getLength());

                                Intent intent = new Intent(CallModeActivity.this, ReceiveCallActivity.class);
                                intent.putExtra(Config.EXTRA_CONTACT_NAME, name);
                                intent.putExtra(Config.EXTRA_IP, address.substring(1, address.length()));
                                IN_CALL = true;
//                                LISTEN = false;
//                                stopCallListener();
                                startActivity(intent);
                            } else {
                                // Receives an invalid request
                                LogHelper.e(TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        } catch (Exception e) {}
                    }
                    LogHelper.e(TAG, "Call Listener ending");
                    socket.disconnect();
                    socket.close();
                } catch (SocketException e) {

                    LogHelper.e(TAG, "SocketException in listener " + e);
                }
            }
        });
        listener.start();
    }

    private void stopCallListener()
    {
        // Ends the listener thread
        LISTEN = false;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (STARTED) {
            deviceManager.bye(deviceUnique);
            deviceManager.stopBroadcasting();
            deviceManager.stopListening();
            //STARTED = false;
        }
        stopCallListener();
        LogHelper.e(TAG, "App paused!");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        LogHelper.e(TAG, "App stopped!");
        stopCallListener();
        if (!IN_CALL) {
            finish();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LogHelper.e(TAG, "App Destroyed!");

        super.onPause();
        if (STARTED) {
            deviceManager.bye(deviceUnique);
            deviceManager.stopBroadcasting();
            deviceManager.stopListening();
        }
        stopCallListener();
        if (!IN_CALL) {
            finish();
        }
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        LogHelper.e(TAG, "App restarted!");
        IN_CALL = false;
        STARTED = true;
        deviceManager = new DeviceManager(deviceName, Networking.getIPAddress(true), Networking.getBroadcastIp());
        deviceManager.setContactUpdateReceiver(context);
        startCallListener();
    }

    @Override
    public void onDeviceUpdate(final DeviceInfo[] deviceInfo)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                updateContactList();
                listViewAdapter.setDeviceInfo(deviceInfo);
            }
        });
    }

    @Override
    public void onDeviceRegistered(final String unique, final boolean registered)
    {
        LogHelper.d(TAG, "onDeviceRegistered: " + unique);
        deviceUnique = unique;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                final TextView textView = (TextView) findViewById(R.id.textViewStatus);
                textView.setText(deviceName);
                textView.setTextColor((registered ? Color.GREEN : Color.GRAY));
            }
        });
    }

    @Override
    public void onListItemClicked(DeviceInfo deviceInfo)
    {
        LogHelper.e(TAG, "ListItem pressed");

        // Collects details about the selected contact
        InetAddress ip = deviceInfo.address;
        IN_CALL = true;

        // Sends this information to the MakeCallActivity and start that activity
        Intent intent = new Intent(context, MakeCallActivity.class);
        intent.putExtra(Config.EXTRA_CONTACT_NAME, deviceInfo.name);
        String address = ip.toString();
        address = address.substring(1, address.length());
        intent.putExtra(Config.EXTRA_IP, address);
        intent.putExtra(Config.EXTRA_DISPLAY_NAME, deviceName);
        startActivity(intent);
    }
}