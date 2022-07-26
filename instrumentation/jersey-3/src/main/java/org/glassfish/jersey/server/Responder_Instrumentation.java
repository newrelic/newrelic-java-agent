/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.glassfish.jersey.server;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "org.glassfish.jersey.server.ServerRuntime$Responder")
class Responder_Instrumentation {

    @NewField
    public Token token;

    @Trace(async = true)
    private ContainerResponse processResponse(ContainerResponse response) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public void process(final Throwable throwable) {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

}
