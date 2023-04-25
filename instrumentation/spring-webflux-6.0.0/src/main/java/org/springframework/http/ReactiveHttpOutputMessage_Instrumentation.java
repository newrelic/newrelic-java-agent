/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.http;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.core.publisher.Mono;

@Weave(type = MatchType.Interface, originalName = "org.springframework.http.ReactiveHttpOutputMessage")
public class ReactiveHttpOutputMessage_Instrumentation {

    @NewField
    public Token token;

    public Mono<Void> setComplete() {
        try {
            if (this.token != null) {
                this.token.expire();
                this.token = null;
            }
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
        return Weaver.callOriginal();
    }
}
