/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Central source of hostname information for the Agent.
 *
 * A reading of one version of the JRE's InetAddress class (Java 7) showed that it does cache hostnames internally,
 * but only for 5 seconds. Agents should ensure that the hostname does not change during the life of an agent run,
 * so we cache here while exposing an API that allows a caller to invalidate the cache when the Agent reconnects.
 */
public class Hostname {

    private static volatile String cachedHostname = null;
    private static volatile String cachedFullHostname = null;
    private static volatile String inUseIPAddress = null;
    private static volatile ArrayList<String> ipAddress = new ArrayList<>();

    private Hostname() {
    }

    /**
     * This checks the configuration property agent_hostname. It then falls back to the default hostname. This should be
     * called for setting the host property. <== I don't know what this means. This method should be called for setting
     * the display_host at connect time. Jeff 10/2016.
     */
    public static String getDisplayHostname(AgentConfig config, final String defaultHostname) {
        return config.getValue("process_host.display_name", defaultHostname);
    }

    /**
     * Optionally invalidate the cache and then return the hostname.
     *
     * This class is not responsible for the caching policy. At the time of this writing,
     * the recommended policy is for the caller to invalidate the cache and redetermine the
     * hostname each time the Agent connects so that the hostname never changes during an
     * agent run. Therefore, this should be called with invalidate true to obtain the value
     * of the default_host property at connect time.
     *
     * @param config The Agent config to used in hostname determination
     * @param invalidate if true, the currently-cached hostname is invalidated
     * @return The hostname
     */
    public static String getHostname(AgentConfig config, boolean invalidate) {
        if (invalidate) {
            cachedHostname = null;
        }
        return getHostname(config);
    }

    /**
     * Return the cached hostname, if any, else determine and cache the hostname and return it.
     * The caching policy is the responsibility of our callers.
     *
     * @param config The Agent config to used in hostname determination
     * @return The hostname
     */
    public static String getHostname(AgentConfig config) {
        if (cachedHostname != null) {
            return cachedHostname;
        }
        try {
            cachedHostname = InetAddress.getLocalHost().getHostName();
            return cachedHostname;
        } catch (UnknownHostException e) {
            // Being here is sort of an unexpected condition despite the fact that it may happen always
            // on some hosts. Since this is unexpected, do not cache the result, so it can clear itself
            // if we got here because of a transient condition.
            Agent.LOG.log(Level.FINE, "Error getting host name. Using IP address.", e);
            return getInUseIpAddress(config);
        }
    }

    public static String getFullHostname(AgentConfig config) {
        if (cachedFullHostname != null) {
            return cachedFullHostname;
        }

        try {
            cachedFullHostname = InetAddress.getLocalHost().getCanonicalHostName();
            return cachedFullHostname;
        } catch (UnknownHostException e) {
            // Being here is sort of an unexpected condition despite the fact that it may happen always
            // on some hosts. Since this is unexpected, do not cache the result, so it can clear itself
            // if we got here because of a transient condition.
            Agent.LOG.log(Level.FINE, "Error getting host name. Using IP address.", e);
            return getInUseIpAddress(config);
        }
    }

    /**
     * This IP Address will either be in ipv4 or ipv6 format determined by the properties:
     * -Djava.net.preferIPv4Addresses=<true|false>
     * -Djava.net.preferIPv6Addresses=<true|false>
     */
    private static InetAddress determineAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            // a random port is used just to get the address
            socket.connect(Inet6Address.getByName("collector.newrelic.com"), 10002);
            return socket.getLocalAddress();
        } catch (SocketException | UnknownHostException e) {
            Agent.LOG.log(Level.FINE, "Unable to determine IP address.", e);
        }
        return InetAddress.getLoopbackAddress();
    }

    // We want the value from InetAddress but, it's more convenient to use InterfaceAddress
    private static List<InterfaceAddress> getAllInterfaceAddresses(String ip) {
        if(ip == null) {
            return Collections.emptyList();
        }
        List<NetworkInterface> networkInterfaces = getNetworkInterfaces();
        for (NetworkInterface networkInterface : networkInterfaces) {
            List<InterfaceAddress> addresses = networkInterface.getInterfaceAddresses();
            for (InterfaceAddress address : addresses) {
                if (ip.equals(InetHostAddress.from(address))) {
                    return addresses;
                }
            }
        }
        return Collections.emptyList();
    }

    @VisibleForTesting
    protected static void setInUseIpAddress(String address) {
        inUseIPAddress = address;
    }

    public static String getInUseIpAddress(AgentConfig config) {
        if (inUseIPAddress == null) {
            discoverInUseIpAddress(config);
        }
        return inUseIPAddress;
    }

    @VisibleForTesting
    protected static void setIpAddress(ArrayList<String> addresses) {
        ipAddress = addresses;
    }

    public static ArrayList<String> getIpAddress(AgentConfig config) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            discoverInUseIpAddress(config);
        }
        return ipAddress;
    }

    private static void discoverInUseIpAddress(AgentConfig config) {
        InetAddress candidate = determineAddress();
        boolean preferIpv6 = preferIpv6(config);

        if (candidate != InetAddress.getLoopbackAddress()) {
            List<InterfaceAddress> iface = getAllInterfaceAddresses(candidate.getHostAddress());
            for (InterfaceAddress addr : iface) {
                ipAddress.add(addr.getAddress().getHostAddress());
                if (preferIpv6) {
                    if (addr.getAddress() instanceof Inet6Address) {
                        inUseIPAddress = addr.getAddress().getHostAddress();
                    }
                } else {
                    if (addr.getAddress() instanceof Inet4Address) {
                        inUseIPAddress = addr.getAddress().getHostAddress();
                    }
                }
            }
        }

        if (inUseIPAddress == null) {
            if (preferIpv6) {
                inUseIPAddress = "0:0:0:0:0:0:0:1";
                Agent.LOG.log(Level.FINE, "Unable to determine IPv6 address. Setting to {0}", inUseIPAddress);
            } else {
                inUseIPAddress = "127.0.0.1";
                Agent.LOG.log(Level.FINE, "Unable to determine IPv4 address. Setting to {0}", inUseIPAddress);
            }
            ipAddress.add(inUseIPAddress);
        }
    }

    private static List<NetworkInterface> getNetworkInterfaces() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns true if IPv6 is preferred, returns false otherwise
     *
     * @param config
     * @return Null if the default of first IP address should be used.
     */
    protected static Boolean preferIpv6(AgentConfig config) {
        Object value = config.getValue("process_host.ipv_preference", null);
        if (value != null) {
            if ("6".equals(String.valueOf(value))) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

}