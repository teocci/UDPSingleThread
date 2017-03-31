package com.github.teocci.udpsimglethread.utils;

import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Base64;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by teocci on 3/20/17.
 */

public class Common
{
    private static final String TAG = LogHelper.makeLogTag(Common.class);

    public static String getDeviceID(ContentResolver contentResolver)
    {
        long deviceID = 0;
        final String string = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        if (string != null) {
            try {
                final BigInteger bi = new BigInteger(string, 16);
                deviceID = bi.longValue();
            } catch (final NumberFormatException ex) {
                // Nothing critical
                LogHelper.i(TAG, ex.toString());
            }
        }

        if (deviceID == 0) {
            // Let's use random number
            deviceID = new Random().nextLong();
        }

        final byte[] bb = new byte[Long.SIZE / Byte.SIZE];
        for (int index = (bb.length - 1); index >= 0; index--) {
            bb[index] = (byte) (deviceID & 0xFF);
            deviceID >>= Byte.SIZE;
        }

        return Base64.encodeToString(bb, (Base64.NO_PADDING | Base64.NO_WRAP));
    }
}
