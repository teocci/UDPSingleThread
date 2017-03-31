package com.github.teocci.udpsimglethread.listeners;

import com.github.teocci.udpsimglethread.model.DeviceInfo;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public interface ContactUpdateReceiver
{
    void onDeviceUpdate(final DeviceInfo[] deviceInfo);

    void onDeviceRegistered(final String unique, boolean registered);
}
