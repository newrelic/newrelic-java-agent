/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.smallrye.mutiny.operators.uni;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SmallRyeUtils;
import io.smallrye.mutiny.subscription.UniSubscriber_Instrumentation;

@Weave(originalName = "io.smallrye.mutiny.operators.uni.UniRunSubscribeOn", type = MatchType.ExactClass)
public class UniRunSubscribeOn_Instrumentation<I> {
    public void subscribe(UniSubscriber_Instrumentation<? super I> subscriber) {
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        Weaver.callOriginal();
    }
}
