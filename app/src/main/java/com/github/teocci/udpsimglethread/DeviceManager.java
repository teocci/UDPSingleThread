package com.github.teocci.udpsimglethread;

import android.util.Base64;

import com.github.teocci.udpsimglethread.listeners.ContactUpdateReceiver;
import com.github.teocci.udpsimglethread.model.DeviceInfo;
import com.github.teocci.udpsimglethread.utils.LogHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public class DeviceManager
{
    public static final String TAG = LogHelper.makeLogTag(DeviceManager.class);

    public static final int BROADCAST_PORT = 50001; // Socket on which packets are sent/received
    private static final int BROADCAST_INTERVAL = 2000; // Milliseconds
    private static final int BROADCAST_BUF_SIZE = 1024;
    private boolean BROADCAST = true;
    private boolean LISTEN = true;
    private HashMap<String, InetAddress> contacts;
    private HashMap<String, DeviceInfo> devices;
    private InetAddress broadcastIP;
    private String deviceAddress;
    private String deviceName;
    private ContactUpdateReceiver contactUpdateReceiver;

    public DeviceManager(String name, String address, InetAddress broadcastIP)
    {
        contacts = new HashMap<>();
        devices = new HashMap<>();
        this.contactUpdateReceiver = null;
        this.deviceName = name;
        this.deviceAddress = address;
        this.broadcastIP = broadcastIP;
        listen();
        broadcastName(name, broadcastIP);
    }

    public HashMap<String, InetAddress> getContacts()
    {
        return contacts;
    }

    public void broadcastName(final String name, final InetAddress broadcastIP)
    {
        // Broadcasts the name of the device at a regular interval
        LogHelper.e(TAG, "Broadcasting started!");
        Thread broadcastThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    String request = "ADD:" + name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
                    while (BROADCAST) {
                        socket.send(packet);
                        LogHelper.e(TAG, "Broadcast packet sent: " + packet.getAddress().toString());
                        Thread.sleep(BROADCAST_INTERVAL);
                    }
                    LogHelper.e(TAG, "Broadcaster ending!");
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {
                    LogHelper.e(TAG, "SocketException broadcast: " + e);
                    LogHelper.e(TAG, "Broadcaster ending!");
                    return;
                } catch (IOException e) {
                    LogHelper.e(TAG, "IOException in broadcast: " + e);
                    LogHelper.e(TAG, "Broadcaster ending!");
                    return;
                } catch (InterruptedException e) {
                    LogHelper.e(TAG, "InterruptedException in broadcast: " + e);
                    LogHelper.e(TAG, "Broadcaster ending!");
                    return;
                }
            }
        });
        broadcastThread.start();
    }

    public void stopBroadcasting()
    {
        // Ends the broadcasting thread
        BROADCAST = false;
    }

    public void listen()
    {
        // Creates the listener thread
        LogHelper.e(TAG, "Listening started!");
        Thread listenThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket(BROADCAST_PORT);
                } catch (SocketException e) {
                    LogHelper.e(TAG, "SocketExcepion in listener: " + e);
                    return;
                }
                byte[] buffer = new byte[BROADCAST_BUF_SIZE];

                while (LISTEN) {
                    listen(socket, buffer);
                }
                LogHelper.e(TAG, "Listener ending!");
                socket.disconnect();
                socket.close();
                return;
            }

            public void listen(DatagramSocket socket, byte[] buffer)
            {
                try {
                    // Listening for new notifications
                    LogHelper.e(TAG, "Listening for a packet!");
                    DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
                    socket.setSoTimeout(15000);
                    socket.receive(packet);
                    String data = new String(buffer, 0, packet.getLength());
                    LogHelper.e(TAG, "Packet received: " + data);
                    String action = data.substring(0, 4);
                    if (action.equals("ADD:")) {
                        // Add notification received. Attempt to add contact
                        LogHelper.e(TAG, "Listener received ADD request");
                        addDevice(data.substring(4, data.length()), packet.getAddress());
                    } else if (action.equals("BYE:")) {
                        // Bye notification received. Attempt to remove contact
                        LogHelper.e(TAG, "Listener received BYE request");
                        removeDevice(data.substring(4, data.length()));
                    } else if (action.equals("REG:")) {
                        // Bye notification received. Attempt to remove contact
                        LogHelper.e(TAG, "Listener received BYE request");
                        addRegistration(data.substring(4, data.length()));
                    } else {
                        // Invalid notification received
                        LogHelper.e(TAG, "Listener received invalid request: " + action);
                    }

                } catch (SocketTimeoutException e) {
                    LogHelper.e(TAG, "No packet received!");
                    if (LISTEN) {
                        listen(socket, buffer);
                    }
                    return;
                } catch (SocketException e) {
                    LogHelper.e(TAG, "SocketException in listen: " + e);
                    LogHelper.e(TAG, "Listener ending!");
                    return;
                } catch (IOException e) {
                    LogHelper.e(TAG, "IOException in listen: " + e);
                    LogHelper.e(TAG, "Listener ending!");
                    return;
                }
            }
        });
        listenThread.start();
    }

    public void stopListening()
    {
        // Stops the listener thread
        LISTEN = false;
    }

    public void addDevice(String name, InetAddress address)
    {
        LogHelper.e(TAG, "deviceName: " + deviceName + " deviceAddress:" + deviceAddress);
        LogHelper.e(TAG, "name: " + name + " address:" + address);
        if (deviceName.equals(name) && deviceAddress.equals(address.getHostAddress())) {
            return;
        }
        String unique = Base64.encodeToString(
                (address.getHostAddress()).getBytes(),
                (Base64.NO_PADDING | Base64.NO_WRAP)
        );

        // If the contact is not already known to us, add it
        if (!contacts.containsKey(unique)) {
            LogHelper.e(TAG, "Adding contact: " + name);
            contacts.put(name, address);
            devices.put(unique, new DeviceInfo(unique, name, address));
            sendRegistration(unique, address);
            LogHelper.e(TAG, "#Contacts: " + contacts.size());
            if (contactUpdateReceiver != null) {
                contactUpdateReceiver.onDeviceUpdate(getStationListLocked());
            }
            return;
        }

        LogHelper.e(TAG, "Contact already exists: " + name);
    }

    public void removeDevice(String name)
    {
        // If the contact is known to us, remove it
        if (devices.containsKey(name)) {
            LogHelper.e(TAG, "Removing contact: " + name);
            devices.remove(name);
            LogHelper.e(TAG, "#Contacts: " + devices.size());
            if (contactUpdateReceiver != null) {
                contactUpdateReceiver.onDeviceUpdate(getStationListLocked());
            }
            return;
        }
        LogHelper.e(TAG, "Cannot remove contact. " + name + " does not exist.");
    }

    public void addRegistration(String unique)
    {
        LogHelper.e(TAG, "The server has register the this device: " + unique + ".");
        String thisUnique = Base64.encodeToString(
                deviceAddress.getBytes(),
                (Base64.NO_PADDING | Base64.NO_WRAP)
        );

        if (contactUpdateReceiver != null) {
            contactUpdateReceiver.onDeviceRegistered(unique, thisUnique.equals(unique));
        }
    }

    public void bye(final String name)
    {
        // Sends a Bye notification to other devices
        Thread byeThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    LogHelper.e(TAG, "Attempting to broadcast BYE notification!");
                    String notification = "BYE:" + name;
                    byte[] message = notification.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
                    socket.send(packet);
                    LogHelper.e(TAG, "Broadcast BYE notification!");
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {
                    LogHelper.e(TAG, "SocketException during BYE notification: " + e);
                } catch (IOException e) {
                    LogHelper.e(TAG, "IOException during BYE notification: " + e);
                }
            }
        });
        byeThread.start();
    }

    public void sendRegistration(final String unique, final InetAddress address)
    {
        // Sends a REG notification to other devices
        Thread byeThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    LogHelper.e(TAG, "Attempting to broadcast REG notification!");
                    String notification = "REG:" + unique;
                    byte[] message = notification.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, address, BROADCAST_PORT);
                    socket.send(packet);
                    LogHelper.e(TAG, "Broadcast REG notification!");
                    socket.disconnect();
                    socket.close();
                    return;
                } catch (SocketException e) {
                    LogHelper.e(TAG, "SocketException during BYE notification: " + e);
                } catch (IOException e) {
                    LogHelper.e(TAG, "IOException during BYE notification: " + e);
                }
            }
        });
        byeThread.start();
    }

    private DeviceInfo[] getStationListLocked()
    {
        final DeviceInfo[] deviceInfo = new DeviceInfo[devices.size()];
        int idx = 0;
        for (Map.Entry<String, DeviceInfo> e : devices.entrySet()) {
                if (e.getValue() != null) {
                    final DeviceInfo serviceInfo = e.getValue();
                    deviceInfo[idx++] = serviceInfo;
                }
        }

        return deviceInfo;
    }

    public void setContactUpdateReceiver(ContactUpdateReceiver contactUpdateReceiver)
    {
        this.contactUpdateReceiver = contactUpdateReceiver;
    }

    public void removeContactUpdateReceiver()
    {
        this.contactUpdateReceiver = null;
    }

    public ContactUpdateReceiver getContactUpdateReceiver()
    {
        return contactUpdateReceiver;
    }
}
