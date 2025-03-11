/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.MonoFlatMap")
abstract class MonoFlatMap_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.MonoFlatMap$FlatMapMain")
    static final class FlatMapMain_Instrumentation<T, R> {

        @NewField
        private Token token;

        @WeaveAllConstructors
        FlatMapMain_Instrumentation() {
            if (AgentBridge.getAgent().getTransaction(false) != null && token == null) {
                Token existingToken = NewRelic.getAgent().getTransaction().getToken();
                token = (existingToken != null && existingToken.isActive()) ? existingToken : NewRelic.getAgent().getTransaction().getToken();
            }
        }

        public void onNext(T t) {
            if (token != null && token.isActive()) {
                token.link();
            }
            Weaver.callOriginal();
        }

        public void onComplete() {
            if (token != null && token.isActive()) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        public void onError(Throwable t) {
            if (token != null && token.isActive()) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        public void cancel() {
            if (token != null && token.isActive()) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }
    }
}