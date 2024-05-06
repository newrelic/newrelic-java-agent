package com.nr.agent.instrumentation.activemqclient580;

import com.newrelic.agent.bridge.messaging.HostAndPort;
import com.newrelic.agent.bridge.messaging.JmsUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActiveMQUtil {
    public static final Pattern addressPattern = Pattern.compile("^\\w+://(.+)/.+:(\\d+)");
    public static HostAndPort parseHostAndPort(String address) {
        HostAndPort cached = JmsUtil.getHostAndPortFromCache(address);
        if (cached != null) {
            return cached;
        }

        Matcher m = addressPattern.matcher(address);
        if(!m.find()) {
            return null;
        }

        String hostName = m.group(1);
        int port;

        try {
            String portStr = m.group(2);
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return null;
        }

        HostAndPort hostAndPort = new HostAndPort(hostName, port);
        return JmsUtil.cacheAndReturnHostAndPort(address, hostAndPort);
    }
}
