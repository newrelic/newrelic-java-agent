/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;

import java.net.URL;
import java.util.Map;

public class TransactionCache {

    // These caches are setup with weak keys.
    private Map<Object, URL> urlCache;
    private Map<Object, MetricNameFormatWithHost> inputStreamCache;

    public MetricNameFormatWithHost getMetricNameFormatWithHost(Object key) {
        return getInputStreamCache().get(key);
    }

    public void putMetricNameFormatWithHost(Object key, MetricNameFormatWithHost val) {
        getInputStreamCache().put(key, val);
    }

    private Map<Object, MetricNameFormatWithHost> getInputStreamCache() {
        if (inputStreamCache == null) {
            inputStreamCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
        }
        return inputStreamCache;
    }

    public URL getURL(Object key) {
        return getUrlCache().get(key);
    }

    public void putURL(Object key, URL val) {
        getUrlCache().put(key, val);
    }

    private Map<Object, URL> getUrlCache() {
        if (urlCache == null) {
            urlCache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
        }
        return urlCache;
    }

}
