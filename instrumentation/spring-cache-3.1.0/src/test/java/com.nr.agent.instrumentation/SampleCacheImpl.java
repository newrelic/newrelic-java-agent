package com.nr.agent.instrumentation;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.HashMap;

public class SampleCacheImpl implements Cache {
    @Override
    public String getName() {
        return "sample";
    }

    @Override
    public Object getNativeCache() {
        return new HashMap<String, Object>();
    }

    @Override
    public ValueWrapper get(Object key) {
        //Simulate miss
        if (key == null) {
            return null;
        }

        //Simulate hit
        return new SimpleValueWrapper("foo");
    }

    @Override
    public void put(Object key, Object value) {

    }

    @Override
    public void evict(Object key) {

    }

    @Override
    public void clear() {

    }
}
