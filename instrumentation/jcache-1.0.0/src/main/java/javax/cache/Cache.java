/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package javax.cache;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface)
public abstract class Cache<K, V> {

    @NewField
    private static final String GET = "get";
    @NewField
    private static final String PUT = "put";
    @NewField
    private static final String REPLACE = "replace";
    @NewField
    private static final String REMOVE = "remove";
    @NewField
    private static final String GET_AND_PUT = "getAndPut";
    @NewField
    private static final String GET_AND_REMOVE = "getAndRemove";
    @NewField
    private static final String GET_AND_REPLACE = "getAndReplace";

    @NewField
    private static final String METRIC_NAME = "/Java/JCACHE";

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + GET, leaf = true)
    public abstract V get(K key);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + PUT, leaf = true)
    public abstract void put(K key, V value);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + GET_AND_PUT, leaf = true)
    public abstract V getAndPut(K key, V value);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + GET_AND_REMOVE, leaf = true)
    public abstract V getAndRemove(K key);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + GET_AND_REPLACE, leaf = true)
    public abstract V getAndReplace(K key, V value);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + REMOVE, leaf = true)
    public abstract boolean remove(K key);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + REMOVE, leaf = true)
    public abstract boolean remove(K key, V oldValue);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + REPLACE, leaf = true)
    public abstract boolean replace(K key, V oldValue);

    @Trace(excludeFromTransactionTrace = true, metricName = METRIC_NAME + "/" + REPLACE, leaf = true)
    public abstract boolean replace(K key, V oldValue, V newValue);

    public abstract String getName();

}
