package com.github.teocci.udpsimglethread.model;

import java.net.InetAddress;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017/Mar/31
 */

public class DeviceInfo
{
    public final String unique;
    public final String name;
    public final InetAddress address;
    public final int transmission;
    public final long ping;

    public DeviceInfo(String unique, String name, InetAddress address)
    {
        this.unique = unique;
        this.name = name;
        this.address = address;
        this.transmission = 0;
        this.ping = 1;
    }
}
