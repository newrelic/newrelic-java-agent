/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.lettuce.core.cluster;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;

@Weave(originalName = "io.lettuce.core.cluster.StatefulRedisClusterConnectionImpl")
public abstract class StatefulRedisClusterConnectionImpl_Instrumentation<K, V> implements StatefulConnection<K, V> {

    @NewField
    public RedisURI firstSeedUri;

}
