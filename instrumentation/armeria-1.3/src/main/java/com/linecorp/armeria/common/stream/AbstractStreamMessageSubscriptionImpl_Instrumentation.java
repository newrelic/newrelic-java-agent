package com.linecorp.armeria.common.stream;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;

@Weave(originalName = "com.linecorp.armeria.common.stream.AbstractStreamMessage$SubscriptionImpl", type = MatchType.ExactClass)
abstract class AbstractStreamMessageSubscriptionImpl_Instrumentation {

//    @Weave(originalName = "com.linecorp.armeria.common.stream.AbstractStreamMessage$SubscriptionImpl", type = MatchType.ExactClass)
//    static class SubscriptionImpl_Instrumentation {

        @WeaveAllConstructors
        AbstractStreamMessageSubscriptionImpl_Instrumentation() {
//        SubscriptionImpl_Instrumentation(AbstractStreamMessage<?> publisher, Subscriber<?> subscriber, EventExecutor executor, boolean withPooledObjects, boolean notifyCancellation) {
            // TODO wrap subscriber
        }

//    }

}
