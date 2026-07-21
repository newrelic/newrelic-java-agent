/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.smallrye.mutiny.operators.multi;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SmallRyeUtils;
import io.smallrye.mutiny.subscription.MultiSubscriber_Instrumentation;

@Weave(originalName = "io.smallrye.mutiny.operators.multi.MultiSubscribeOnOp", type = MatchType.ExactClass)
public class MultiSubscribeOnOp_Instrumentation<T> {
    public void subscribe(MultiSubscriber_Instrumentation<? super T> subscriber) {
        SmallRyeUtils.assignTokenToSubscriber(subscriber);
        Weaver.callOriginal();
    }
}
