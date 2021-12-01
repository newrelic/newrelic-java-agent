package com.newrelic.agent.config;

import java.net.InetAddress;
import java.net.InterfaceAddress;

public class InetHostAddress {
    public static String from(InterfaceAddress address) {
        if(address == null) {
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        if(inetAddress == null) {
            return null;
        }
        return address.getAddress().getHostAddress();
    }
}
