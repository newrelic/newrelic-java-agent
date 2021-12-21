/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;

import java.net.URL;

public class TransactionCache {

    // These caches are setup with weak keys.
    private Cache<Object, URL> urlCache;
    private Cache<Object, MetricNameFormatWithHost> inputStreamCache;

    public MetricNameFormatWithHost getMetricNameFormatWithHost(Object key) {
        return getInputStreamCache().getIfPresent(key);
    }

    public void putMetricNameFormatWithHost(Object key, MetricNameFormatWithHost val) {
        getInputStreamCache().put(key, val);
    }

    private Cache<Object, MetricNameFormatWithHost> getInputStreamCache() {
        if (inputStreamCache == null) {
            inputStreamCache = Caffeine.newBuilder().weakKeys().executor(Runnable::run).build();
        }
        return inputStreamCache;
    }

    public URL getURL(Object key) {
        return (URL) getUrlCache().getIfPresent(key);
    }

    public void putURL(Object key, URL val) {
        getUrlCache().put(key, val);
    }

    private Cache<Object, URL> getUrlCache() {
        if (urlCache == null) {
            urlCache = Caffeine.newBuilder().weakKeys().executor(Runnable::run).build();
        }
        return urlCache;
    }

}
