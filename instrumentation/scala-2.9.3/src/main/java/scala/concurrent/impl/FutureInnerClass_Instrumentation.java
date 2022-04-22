/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package scala.concurrent.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.scala.ScalaUtils;
import com.nr.agent.instrumentation.scala.WrappedFunction0;
import scala.Function0;
import scala.concurrent.ExecutionContext;

import java.util.concurrent.atomic.AtomicInteger;

@Weave(originalName = "scala.concurrent.impl.Future$")
public class FutureInnerClass_Instrumentation {

    public <T> scala.concurrent.Future<T> apply(Function0<T> body, final ExecutionContext executor) {
      AgentBridge.TokenAndRefCount tokenAndRefCount =
        !body.getClass().getName().startsWith("akka.") ? ScalaUtils.getThreadTokenAndRefCount() : null;

        if (tokenAndRefCount != null) {
              body = new WrappedFunction0(body, tokenAndRefCount);
        }
        scala.concurrent.Future<T> value = Weaver.callOriginal();

        return value;
    }
}
