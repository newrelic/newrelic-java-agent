/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
            inputStreamCache = CacheBuilder.newBuilder().weakKeys().build();
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
            urlCache = CacheBuilder.newBuilder().weakKeys().build();
        }
        return urlCache;
    }

}
