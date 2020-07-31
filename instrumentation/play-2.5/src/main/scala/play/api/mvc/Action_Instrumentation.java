/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package play.api.mvc;

import akka.util.ByteString;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import play.api.libs.streams.Accumulator;
import scala.concurrent.Future;

@Weave(type = MatchType.Interface, originalName = "play.api.mvc.Action")
public class Action_Instrumentation {

    @NewField
    private Token token;

    @Trace(async = true, metricName = "Play2Controller")
    public Future<Result> apply(Request request) {
        if (this.token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    public Accumulator<ByteString, Result> apply(RequestHeader rh) {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null && token == null) {
            token = transaction.getToken();
        }
        return Weaver.callOriginal();
    }

}
