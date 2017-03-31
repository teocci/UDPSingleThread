package com.github.teocci.udpsimglethread.utils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by teocci on 3/9/17.
 */

public class Networking
{
    static final String TAG = LogHelper.makeLogTag(Networking.class);

    /**
     * Convert byte array to hex string
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes)
    {
        StringBuilder sbuf = new StringBuilder();
        for (int idx = 0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     *
     * @param str
     * @return array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str)
    {
        try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public static String loadFileAsString(String filename) throws IOException
    {
        final int BUFLEN = 1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8 = false;
            int read, count = 0;
            while ((read = is.read(bytes)) != -1) {
                if (count == 0 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                    isUTF8 = true;
                    baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count += read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try { is.close(); } catch (Exception ex) {}
        }
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName)
    {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface anInterface : interfaces) {
                if (interfaceName != null) {
                    if (!anInterface.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = anInterface.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4)
    {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface anInterface : interfaces) {
                List<InetAddress> addresses = Collections.list(anInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String hostAddress = address.getHostAddress();
//                        boolean isIPv4 = InetAddressUtils.isIPv4Address(hostAddress);
                        boolean isIPv4 = hostAddress.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return hostAddress;
                        } else {
                            if (!isIPv4) {
                                int delim = hostAddress.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? hostAddress.toUpperCase() : hostAddress.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public static InetAddress getBroadcastIp()
    {
        // Function to return the broadcast address, based on the IP address of the device
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface anInterface : interfaces) {
                List<InetAddress> addresses = Collections.list(anInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String addressString = address.getHostAddress();
//                        boolean isIPv4 = InetAddressUtils.isIPv4Address(hostAddress);

                        Log.e(TAG, "[getBroadcastIp]addressString: " + addressString);
                        boolean isIPv4 = addressString.indexOf(':') < 0;
                        if (isIPv4) {
                            int endIndex = addressString.lastIndexOf(".");
                            addressString = addressString.substring(0, endIndex) + ".255";

                            Log.e(TAG, "[getBroadcastIp]addressStringBroadcast: " + addressString);
                            return InetAddress.getByName(addressString);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "[getBroadcastIp]UnknownHostException in getBroadcastIP: " + ex);
            return null;
        }
        return null;
    }

    private String toBroadcastIp(int ip)
    {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }

    private static int getAvailablePort() throws IOException
    {
        Random RANDOM = new Random();
        int port;
        do {
            port = RANDOM.nextInt(20000) + 10000;
        } while (!isPortAvailable(port));

        return port;
    }

    public static boolean isPortAvailable(final int port) throws IOException
    {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (ss != null) {
                ss.close();
            }
        }

        return false;
    }
}
