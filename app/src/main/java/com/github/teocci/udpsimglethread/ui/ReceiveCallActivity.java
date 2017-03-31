package com.github.teocci.udpsimglethread.ui;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.github.teocci.udpsimglethread.AudioCall;
import com.github.teocci.udpsimglethread.R;
import com.github.teocci.udpsimglethread.utils.Config;
import com.github.teocci.udpsimglethread.utils.LogHelper;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public class ReceiveCallActivity extends Activity
{
    public static final String TAG = LogHelper.makeLogTag(ReceiveCallActivity.class);
    
    private static final int BROADCAST_PORT = 50002;
    private static final int BUF_SIZE = 1024;
    private String deviceIp;
    private String deviceName;
    private boolean LISTEN = true;
    private boolean IN_CALL = false;
    private AudioCall call;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_call);

        Intent intent = getIntent();
        deviceName = intent.getStringExtra(Config.EXTRA_CONTACT_NAME);
        deviceIp = intent.getStringExtra(Config.EXTRA_IP);

        TextView textView = (TextView) findViewById(R.id.textViewIncomingCall);
        textView.setText("Incoming call: " + deviceName);

        final Button endButton = (Button) findViewById(R.id.buttonEndCall1);
        endButton.setVisibility(View.INVISIBLE);

        startListener();

        Button acceptButton = (Button) findViewById(R.id.buttonAccept);
        acceptButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try {
                    // Accepts a call. Send a notification and start the call
                    sendMessage("ACC:");
                    InetAddress address = InetAddress.getByName(deviceIp);
                    LogHelper.e(TAG, "Calling " + address.toString());
                    IN_CALL = true;
                    call = new AudioCall(address);
                    call.startCall();
                    // Hides the buttons as they're not longer required
                    Button accept = (Button) findViewById(R.id.buttonAccept);
                    accept.setEnabled(false);

                    Button reject = (Button) findViewById(R.id.buttonReject);
                    reject.setEnabled(false);

                    endButton.setVisibility(View.VISIBLE);
                } catch (UnknownHostException e) {

                    LogHelper.e(TAG, "UnknownHostException in acceptButton: " + e);
                } catch (Exception e) {

                    LogHelper.e(TAG, "Exception in acceptButton: " + e);
                }
            }
        });

        Button rejectButton = (Button) findViewById(R.id.buttonReject);
        rejectButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Sends a reject notification and end the call
                sendMessage("REJ:");
                endCall();
            }
        });

        endButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                endCall();
            }
        });
    }

    private void endCall()
    {
        // End the call and send a notification
        stopListener();
        if (IN_CALL) {
            call.endCall();
        }
        sendMessage("END:");
        finish();
    }

    private void startListener()
    {
        // Creates the listener thread
        LISTEN = true;
        Thread listenThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    LogHelper.e(TAG, "Listener started!");
                    DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
                    socket.setSoTimeout(1500);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while (LISTEN) {
                        try {
                            LogHelper.e(TAG, "Listening for packets");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            LogHelper.e(TAG, "Packet received from " + packet.getAddress() + " with contents: " + data);
                            String action = data.substring(0, 4);
                            if (action.equals("END:")) {
                                // End call notification received. End call
                                endCall();
                            } else {
                                // Invalid notification received.
                                LogHelper.e(TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        } catch (IOException e) {
                            LogHelper.e(TAG, "IOException in Listener " + e);
                        }
                    }
                    LogHelper.e(TAG, "Listener ending");
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {
                    LogHelper.e(TAG, "SocketException in Listener " + e);
                    endCall();
                }
            }
        });
        listenThread.start();
    }

    private void stopListener()
    {
        // Ends the listener thread
        LISTEN = false;
    }

    private void sendMessage(final String message)
    {
        // Creates a thread for sending notifications
        Thread replyThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    InetAddress address = InetAddress.getByName(deviceIp);
                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, BROADCAST_PORT);
                    socket.send(packet);
                    LogHelper.e(TAG, "Sent message( " + message + " ) to " + deviceIp);
                    socket.disconnect();
                    socket.close();
                } catch (UnknownHostException e) {
                    LogHelper.e(TAG, "Failure. UnknownHostException in sendMessage: " + deviceIp);
                } catch (SocketException e) {
                    LogHelper.e(TAG, "Failure. SocketException in sendMessage: " + e);
                } catch (IOException e) {
                    LogHelper.e(TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflates the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
}