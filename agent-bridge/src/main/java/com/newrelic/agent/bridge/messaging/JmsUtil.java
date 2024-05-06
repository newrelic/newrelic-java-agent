package com.newrelic.agent.bridge.messaging;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.Map;

public class JmsUtil {
    private static final Map<Object, Object> hostAndPortCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static final String JMS_HOST_AND_PORT_PROPERTY = "com.nr.agent.instrumentation.jms.HostAndPort";

    public static HostAndPort getHostAndPortFromCache(String address) {
        Object obj = hostAndPortCache.get(address);
        if (obj instanceof HostAndPort) {
            return (HostAndPort) obj;
        }
        if (obj != null) {
            hostAndPortCache.remove(address);
        }
        return null;
    }

    public static HostAndPort cacheAndReturnHostAndPort(String address, HostAndPort hostAndPort) {
        hostAndPortCache.put(address, hostAndPort);
        return hostAndPort;
    }
}
