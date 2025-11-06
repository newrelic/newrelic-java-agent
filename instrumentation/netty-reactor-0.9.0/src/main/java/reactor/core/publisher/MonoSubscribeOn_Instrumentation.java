/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.MonoSubscribeOn")
abstract class MonoSubscribeOn_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "reactor.core.publisher.MonoSubscribeOn$SubscribeOnSubscriber")
    static final class SubscribeOnSubscriber_Instrumentation<T> {
        @NewField
        private Token token;

        @WeaveAllConstructors
        SubscribeOnSubscriber_Instrumentation() {
            if (NewRelic.getAgent().getTransaction() != null && token == null) {
                token = NewRelic.getAgent().getTransaction().getToken();
            }
        }

        public void run () {
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

        public void onComplete() {
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

        public void cancel () {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }
    }
}
