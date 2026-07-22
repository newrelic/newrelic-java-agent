/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package io.smallrye.mutiny.subscription;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SmallRyeUtils;

@Weave(originalName = "io.smallrye.mutiny.subscription.UniSubscriber", type = MatchType.Interface)
public class UniSubscriber_Instrumentation<T> {
    @Trace(async = true)
    public void onFailure(Throwable t) {
        Token token = SmallRyeUtils.getToken(this);
        if (token != null) {
            NewRelic.noticeError(t);
            token.linkAndExpire();
            SmallRyeUtils.removeToken(this);
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void onItem(T x) {
        Token token = SmallRyeUtils.getToken(this);
        if (token != null) {
            token.linkAndExpire();
            SmallRyeUtils.removeToken(this);
        }

        Weaver.callOriginal();
    }
}
