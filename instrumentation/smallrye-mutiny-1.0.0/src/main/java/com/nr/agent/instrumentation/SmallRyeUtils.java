/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;


import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import java.util.Map;

public class SmallRyeUtils {
    // This replaces the usage of @NewField in the Subscriber weave classes
    private static final Map<Object, Token> tokenMap = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    public static void assignTokenToSubscriber(Object subscriber) {
        if (subscriber != null) {
            if (getToken(subscriber) == null) {
                Token token = NewRelic.getAgent().getTransaction().getToken();
                if (token.isActive()) {
                    putToken(subscriber, token);
                } else {
                    token.expire();
                }
            }
        }
    }

    public static void putToken(Object key, Token token) {
        tokenMap.put(key, token);
    }

    public static Token getToken(Object key) {
        return tokenMap.get(key);
    }

    // Here to allow proactive removal of tokens if possible
    public static void removeToken(Object key) {
        tokenMap.remove(key);
    }
}
