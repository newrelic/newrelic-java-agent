package com.newrelic.agent.config;

import java.net.InterfaceAddress;

public class InterfaceAddressAccessor {
    public static String getInetHostAddressFrom(InterfaceAddress address) {
        if(address == null) {
            return null;
        }
        return address.getAddress().getHostAddress();
    }
}
