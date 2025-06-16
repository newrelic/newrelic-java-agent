/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import org.reactivestreams.Subscription;

@Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.MonoFlatMap")
abstract class MonoFlatMap_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.MonoFlatMap$FlatMapMain")
    static final class FlatMapMain_Instrumentation<T, R> {

        @NewField
        private Token token;

        @WeaveAllConstructors
        FlatMapMain_Instrumentation() {}

        public void onSubscribe(Subscription s) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null && tx.isTransactionNameSet() && token == null) {
                token = tx.getToken();
                if (token != null && token.isActive()) {
                    token.link();
                }
            }
            Weaver.callOriginal();
        }

        @Trace(async = true)
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