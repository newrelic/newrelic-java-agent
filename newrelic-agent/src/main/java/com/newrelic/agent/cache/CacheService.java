/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cache;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.util.MethodCache;
import com.newrelic.agent.util.SingleClassLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CacheService extends AbstractService implements HarvestListener {

    private static final long CLEAR_CACHE_INTERVAL = TimeUnit.NANOSECONDS.convert(600, TimeUnit.SECONDS);

    private final ConcurrentMap<String, SingleClassLoader> singleClassLoaders = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassMethodSignature, MethodCache> methodCaches = new ConcurrentHashMap<>();

    private final String defaultAppName;
    private volatile long lastTimeCacheCleared = System.nanoTime();

    public CacheService() {
        super(CacheService.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    }

    @Override
    protected void doStart() throws Exception {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void afterHarvest(String appName) {
        if (!appName.equals(defaultAppName)) {
            return;
        }
        long timeNow = System.nanoTime();
        if ((timeNow - lastTimeCacheCleared) < CLEAR_CACHE_INTERVAL) {
            return;
        }
        try {
            clearCaches();
        } finally {
            lastTimeCacheCleared = timeNow;
        }
    }

    private void clearCaches() {
        for (SingleClassLoader singleClassLoader : singleClassLoaders.values()) {
            singleClassLoader.clear();
        }
        for (MethodCache methodCache : methodCaches.values()) {
            methodCache.clear();
        }
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        // do nothing
    }

    public SingleClassLoader getSingleClassLoader(String className) {
        SingleClassLoader singleClassLoader = singleClassLoaders.get(className);
        if (singleClassLoader != null) {
            return singleClassLoader;
        }
        singleClassLoader = new SingleClassLoader(className);
        SingleClassLoader oldSingleClassLoader = singleClassLoaders.putIfAbsent(className, singleClassLoader);
        return oldSingleClassLoader == null ? singleClassLoader : oldSingleClassLoader;
    }

    public MethodCache getMethodCache(String className, String methodName, String methodDesc) {
        ClassMethodSignature key = new ClassMethodSignature(className.replace('/', '.'), methodName, methodDesc);
        MethodCache methodCache = methodCaches.get(key);
        if (methodCache != null) {
            return methodCache;
        }
        methodCache = new MethodCache(methodName);
        MethodCache oldMethodCache = methodCaches.putIfAbsent(key, methodCache);
        return oldMethodCache == null ? methodCache : oldMethodCache;
    }

}
