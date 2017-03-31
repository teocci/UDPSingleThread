package com.github.teocci.udpsimglethread.io;

import com.github.teocci.udpsimglethread.ui.CallModeActivity;
import com.github.teocci.udpsimglethread.utils.LogHelper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public class ServerManager
{
//    public static final String TAG = LogHelper.makeLogTag(CallModeActivity.class);
//
//    private static final int LISTENER_PORT = 50003;
//    private static final int BROADCAST_INTERVAL = 10000; // Milliseconds
//    private static final int BUF_SIZE = 1024;
//
//
//    private boolean BROADCAST = true;
//    private boolean isRunning = true;
//
//    private void startCallListener()
//    {
//        // Creates the listener thread
//        isRunning = true;
//        Thread listener = new Thread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                try {
//                    // Set up the socket and packet to receive
//                    LogHelper.e(TAG, "Incoming call listener started");
//                    DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
//                    socket.setSoTimeout(1000);
//                    byte[] buffer = new byte[BUF_SIZE];
//                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
//                    while (isRunning) {
//                        // Listen for incoming call requests
//                        try {
//                            LogHelper.e(TAG, "Listening for incoming calls");
//                            socket.receive(packet);
//                            String data = new String(buffer, 0, packet.getLength());
//                            LogHelper.e(TAG, "Packet received from " + packet.getAddress() + " with contents: " + data);
//                            String action = data.substring(0, 4);
//                            if (action.equals("CAL:")) {
//                                // Received a call request. Start the ReceiveCallActivity
//                                String address = packet.getAddress().toString();
//                                String name = data.substring(4, packet.getLength());
//
//                                Intent intent = new Intent(MainActivity.this, ReceiveCallActivity.class);
//                                intent.putExtra(EXTRA_CONTACT, name);
//                                intent.putExtra(EXTRA_IP, address.substring(1, address.length()));
//                                IN_CALL = true;
//                                //isRunning = false;
//                                //stopCallListener();
//                                startActivity(intent);
//                            } else {
//                                // Received an invalid request
//                                Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
//                            }
//                        } catch (Exception e) {}
//                    }
//                    LogHelper.e(TAG, "Call Listener ending");
//                    socket.disconnect();
//                    socket.close();
//                } catch (SocketException e) {
//
//                    Log.e(LOG_TAG, "SocketException in listener " + e);
//                }
//            }
//        });
//        listener.start();
//    }
//
//    private void stopCallListener()
//    {
//        // Ends the listener thread
//        isRunning = false;
//    }
}
