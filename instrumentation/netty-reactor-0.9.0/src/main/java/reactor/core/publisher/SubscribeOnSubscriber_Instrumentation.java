/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.scheduler.Scheduler;

@Weave(originalName = "reactor.core.publisher.MonoSubscribeOn$SubscribeOnSubscriber")
final class SubscribeOnSubscriber_Instrumentation<T> {

    @NewField
    Token token;
    SubscribeOnSubscriber_Instrumentation(Publisher<? extends T> parent,
            CoreSubscriber<? super T> actual,
            Scheduler.Worker worker) {
        if (NewRelic.getAgent().getTransaction() != null && token == null) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
    }

    public void run() {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

    public void cancel() {
        if (token != null) {
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }
}
