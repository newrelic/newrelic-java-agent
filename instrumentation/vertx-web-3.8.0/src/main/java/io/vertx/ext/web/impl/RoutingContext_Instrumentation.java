/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.ext.web.impl;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.nr.vertx.instrumentation.VertxUtil;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import static com.nr.vertx.instrumentation.VertxUtil.NEWRELIC_TOKEN;

@Weave(originalName = "io.vertx.ext.web.impl.RoutingContextImpl")
public abstract class RoutingContext_Instrumentation {

    @WeaveAllConstructors
    RoutingContext_Instrumentation() {
        if (get(NEWRELIC_TOKEN) == null) {
            put(NEWRELIC_TOKEN, NewRelic.getAgent().getTransaction().getToken());
            addHeadersEndHandler(VertxUtil.expireAndNameTxnHandler((RoutingContext) this));
        }
    }

    public abstract RoutingContext put(String key, Object obj);

    public abstract <T> T get(String key);

    public abstract int addHeadersEndHandler(Handler<Void> handler);
}
