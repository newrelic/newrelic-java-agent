/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.cache;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.cache.Cache;

import java.util.logging.Level;

@Weave(type = MatchType.Interface, originalName = "org.springframework.cache.Cache")
public abstract class Cache_Instrumentation {
    @NewField
    private static final String PREFIX = "Cache/Spring/";

    public Cache.ValueWrapper get(Object key) {
        Cache.ValueWrapper result = Weaver.callOriginal();
        NewRelic.incrementCounter(PREFIX + getProviderClassName() + "/" + getName() + (result == null ? "/misses" : "/hits"));
        return result;
    }

    public void evict(Object key) {
        Weaver.callOriginal();
        NewRelic.incrementCounter(PREFIX + getProviderClassName() + "/" + getName() + "/evict");
    }

    public void clear() {
        Weaver.callOriginal();
        NewRelic.incrementCounter(PREFIX + getProviderClassName() + "/" + getName() + "/clear");
    }

    public abstract Object getNativeCache();

    public abstract String getName();

    private String getProviderClassName() {
        Object provider = getNativeCache();
        if (provider != null) {
            return provider.getClass().getName();
        } else {
            return "Unknown";
        }
    }
}
