/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.FluxMapFuseable")
abstract class FluxMapFuseable_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber")
    static final class MapFuseableSubscriber_Instrumentation<T, R> {
        @NewField
        private Token token;

        @WeaveAllConstructors
        MapFuseableSubscriber_Instrumentation() {
            if (AgentBridge.getAgent().getTransaction(false) != null && token == null) {
                token = NewRelic.getAgent().getTransaction().getToken();
            }
        }

        public void onComplete() {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        public void onNext(T t) {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        public void onError(Throwable t) {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }
    }
}
