package com.github.teocci.udpsimglethread.utils;

import android.media.AudioManager;

/**
 * Created by teocci on 3/17/17.
 */

public class Config
{
    public static final int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
    public static int PING_INTERVAL = 5;
    public static final int SAMPLE_INTERVAL = 20; // Milliseconds
    public static final int SAMPLE_SIZE = 2; // Bytes

    public final static String EXTRA_CONTACT_NAME = "com.github.teocci.udpsimglethread.CONTACT";
    public final static String EXTRA_IP = "com.github.teocci.udpsimglethread.IP";
    public final static String EXTRA_DISPLAY_NAME = "com.github.teocci.udpsimglethread.DISPLAY_NAME";
    public final static String EXTRA_OPERATION_MODE = "com.github.teocci.udpsimglethread.OPERATION_MODE";

    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_DEVICE_NAME_LIST = "device_name_list";

    public static final String KEY_VOLUME = "volume";
    public static final String KEY_CHECK_WIFI_STATUS = "check-wifi-status";
    public static final String KEY_USE_VOLUME_BUTTONS_TO_TALK = "use-volume-buttons-to-talk";


    public static final String SERVER_MODE = "server_mode";
    public static final String CLIENT_MODE = "client_mode";
    public static final String SERVICE_TYPE = "_simplensd._tcp"; // Smart Mixer
    public static final String SERVICE_NAME = "NSDService";
    public static final String SERVICE_NAME_SEPARATOR = ":";
}
