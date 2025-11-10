/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.activemqclient580;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.messaging.BrokerInstance;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActiveMQUtil {

    private static final ActiveMQUtil INSTANCE = ActiveMQUtil.create();

    public static ActiveMQUtil get() {
        return INSTANCE;
    }

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\w+://(.+)/.+:(\\d+)");
    private final Function<String, BrokerInstance> DO_PARSE_HOST_REF = this::doParseHostAndPort;
    private final Function<String, BrokerInstance> CACHE = AgentBridge.collectionFactory.memorize(DO_PARSE_HOST_REF, 32);

    public BrokerInstance parseHostAndPort(String address) {
        return CACHE.apply(address);
    }
    public BrokerInstance doParseHostAndPort(String address) {

        Matcher m = ADDRESS_PATTERN.matcher(address);
        if(!m.find()) {
            return BrokerInstance.empty();
        }

        String hostName = m.group(1);
        int port;

        try {
            String portStr = m.group(2);
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return BrokerInstance.empty();
        }
        return new BrokerInstance(hostName, port);
    }

    private ActiveMQUtil() {
        // prevent instantiation of utility class
    }

    private static ActiveMQUtil create() {
        return new ActiveMQUtil();
    }
}