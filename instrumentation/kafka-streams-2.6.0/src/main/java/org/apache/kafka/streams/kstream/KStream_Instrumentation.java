/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.streams.kstream;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "org.apache.kafka.streams.kstream.KStream")
public abstract class KStream_Instrumentation<K, V> {

    // Prevents kafka-streams versions 0.10.2.2 and earlier from applying
    public KStream<K, V> repartition(final Repartitioned<K, V> repartitioned) {
        return Weaver.callOriginal();
    }
}
