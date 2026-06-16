/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.camel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.apache.camel.CamelUtil;
import com.nr.instrumentation.apache.camel.processors.ExchangeProcessor;

@Weave(originalName = "org.apache.camel.AsyncProcessor", type = MatchType.Interface)
public class AsyncProcessor_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public boolean process(Exchange exchange, AsyncCallback callback) {
        if (exchange instanceof Exchange_Instrumentation) {
            Exchange_Instrumentation exchangeInstrumentation = (Exchange_Instrumentation) exchange;
            if (exchangeInstrumentation.token != null) {
                exchangeInstrumentation.token.link();
            } else if (exchangeInstrumentation.fromConsumer) {
                ExchangeProcessor exchangeProcessor = CamelUtil.getExchangeProcessor(CamelUtil.getEndpoint(exchange));
                if (exchangeProcessor != null && exchangeProcessor.shouldStartTransaction()) {
                    CamelUtil.startTxn(exchange, exchangeProcessor);
                }
            }
        }

        return Weaver.callOriginal();
    }
}
