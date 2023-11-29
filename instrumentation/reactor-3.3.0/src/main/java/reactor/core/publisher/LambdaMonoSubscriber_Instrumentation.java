/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.util.context.Context;

@Weave(originalName = "reactor.core.publisher.LambdaMonoSubscriber")
abstract class LambdaMonoSubscriber_Instrumentation {
    @NewField
    private Context nrContext;
    final Context initialContext = Weaver.callOriginal();

    @WeaveAllConstructors
    protected LambdaMonoSubscriber_Instrumentation() {
        // LamdaMonoSubscriber creates a new Context, so we create a new token and put it on the Context
        // to be linked by TokenLinkingSubscriber but expired on onComplete here
        if (AgentBridge.getAgent().getTransaction(false) != null
                && initialContext.getOrDefault("newrelic-token", null) == null) {
            nrContext = Context.of("newrelic-token", NewRelic.getAgent().getTransaction().getToken());
        }
    }

    public final void onComplete() {
        Token token = this.currentContext().getOrDefault("newrelic-token", null);
        if (token != null) {
            token.expire();
            this.nrContext = null;
        }
        Weaver.callOriginal();
    }

    public final void onError(Throwable t) {
        Token token = this.currentContext().getOrDefault("newrelic-token", null);
        if (token != null) {
            token.expire();
            this.nrContext = null;
        }
        Weaver.callOriginal();
    }

    public Context currentContext() {
        if (nrContext != null) {
            return initialContext.putAll(nrContext);
        }
        return Weaver.callOriginal();
    }

}
