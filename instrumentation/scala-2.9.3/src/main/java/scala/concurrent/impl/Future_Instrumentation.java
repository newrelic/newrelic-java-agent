/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package scala.concurrent.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.scala.ScalaUtils;
import com.nr.agent.instrumentation.scala.WrappedFunction0;
import scala.Function0;

import static com.nr.agent.instrumentation.scala.ScalaUtils.scalaFuturesAsSegments;

public class Future_Instrumentation<T> {

    @Weave(originalName = "scala.concurrent.impl.Future$PromiseCompletingRunnable")
    public static class PromiseCompletingRunnable_Instrumentation {

        private final Function0<?> body = Weaver.callOriginal();

        @Trace(excludeFromTransactionTrace = true)
        public void run() {
            Segment segment = null;
            WrappedFunction0 wrapped = null;
            boolean remove = false;

            if (body instanceof WrappedFunction0) {
                wrapped = (WrappedFunction0) body;

                // If we are here and there is no activeToken in progress we are the first one so we set this boolean in
                // order to correctly remove the "activeToken" from the thread local after the original run() method executes
                remove = AgentBridge.activeToken.get() == null;
                AgentBridge.activeToken.set(wrapped.tokenAndRefCount);

                if (scalaFuturesAsSegments && remove) {
                    Transaction tx = AgentBridge.getAgent().getTransaction(false);
                    if (tx != null) {
                        segment = tx.startSegment("Scala", "Future");
                        segment.setMetricName("Scala", "Future", ScalaUtils.nameScalaFunction(wrapped.original.getClass().getName()));
                    }
                }
            }

            try {
                Weaver.callOriginal();
            } finally {
                if (wrapped != null) {
                    if (segment != null) {
                        segment.end();
                    }

                    if (remove) {
                        AgentBridge.activeToken.remove();
                    }
                   ScalaUtils.clearThreadTokenAndRefCountAndTxn(wrapped.tokenAndRefCount);
                }
            }
        }

    }
}
