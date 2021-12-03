package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.logging.Level;

public class InetHostAddress {
    public static String from(InterfaceAddress address) {
        if(address == null) {
            Agent.LOG.log(Level.FINE, "Unable to determine host address from null InterfaceAddress.");
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        if(inetAddress == null) {
            Agent.LOG.log(Level.FINE, "Unable to determine host address from null InetAddress.");
            return null;
        }
        return address.getAddress().getHostAddress();
    }
}
