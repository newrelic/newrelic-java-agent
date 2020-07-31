/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package grails.async;

import groovy.lang.Closure;

import java.util.List;
import java.util.Map;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class Promises {

    public static <T> Promise<T> createPromise(Closure<T>... closures) {
        for (Closure<T> closure : closures) {
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(closure);
        }
        return Weaver.callOriginal();
    }

    public static <T> Promise<T> task(Closure<T> c) {
        AgentBridge.getAgent().getTransaction().registerAsyncActivity(c);
        return Weaver.callOriginal();
    }

    public static <T> Promise<List<T>> tasks(List<Closure<T>> closures) {
        for (Closure<T> c : closures) {
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(c);
        }
        return Weaver.callOriginal();
    }

    public static <K, V> Promise<Map<K, V>> tasks(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            AgentBridge.getAgent().getTransaction().registerAsyncActivity(entry.getValue());
        }
        return Weaver.callOriginal();
    }
}
