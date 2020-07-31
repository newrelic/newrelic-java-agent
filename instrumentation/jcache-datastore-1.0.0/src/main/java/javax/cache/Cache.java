/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.cache;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface)
public abstract class Cache<K, V> {
    @NewField
    private static final String GET = "get";
    @NewField
    private static final String PUT = "put";
    @NewField
    private static final String GET_AND_PUT = "getAndPut";
    @NewField
    private static final String GET_AND_REMOVE = "getAndRemove";
    @NewField
    private static final String GET_AND_REPLACE = "getAndReplace";
    @NewField
    private static final String REPLACE = "replace";
    @NewField
    private static final String REMOVE = "remove";
    @NewField
    private static final String CreateCache = "createcache";

    @Trace(leaf = true)
    public V get(K key) {
        instrument(NewRelic.getAgent().getTracedMethod(), GET);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public void put(K key, V value) {
        instrument(NewRelic.getAgent().getTracedMethod(), PUT);
        Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public V getAndPut(K key, V value) {
        instrument(NewRelic.getAgent().getTracedMethod(), GET_AND_PUT);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public V getAndRemove(K key) {
        instrument(NewRelic.getAgent().getTracedMethod(), GET_AND_REMOVE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public V getAndReplace(K key, V value) {
        instrument(NewRelic.getAgent().getTracedMethod(), GET_AND_REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public boolean remove(K key) {
        instrument(NewRelic.getAgent().getTracedMethod(), REMOVE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public boolean remove(K key, V oldValue) {
        instrument(NewRelic.getAgent().getTracedMethod(), REMOVE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public boolean replace(K key, V oldValue) {
        instrument(NewRelic.getAgent().getTracedMethod(), REPLACE);
        return Weaver.callOriginal();
    }

    @Trace(leaf = true)
    public boolean replace(K key, V oldValue, V newValue) {
        instrument(NewRelic.getAgent().getTracedMethod(), REPLACE);
        return Weaver.callOriginal();
    }

    public abstract String getName();

    private void instrument(TracedMethod method, String commandName) {
        NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
                .product(DatastoreVendor.JCache.name())
                .collection(getName())
                .operation(commandName)
                .noInstance()
                .build());
    }
}
