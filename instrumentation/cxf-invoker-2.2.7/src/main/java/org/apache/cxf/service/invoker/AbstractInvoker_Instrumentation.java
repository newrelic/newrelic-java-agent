/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.cxf.service.invoker;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.EndpointInfo;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.cxfinvoker.CxfInvokerUtils;

@Weave(originalName = "org.apache.cxf.service.invoker.AbstractInvoker", type = MatchType.ExactClass)
public class AbstractInvoker_Instrumentation {

    @Trace
    protected Object performInvocation(Exchange exchange, Object serviceObject, Method method,
            Object[] paramArray) throws Exception {
        Transaction transaction = AgentBridge.getAgent().getTransaction(false);
        if (transaction != null) {
            transaction.getTracedMethod().setMetricName("Java", serviceObject.getClass().getName(), method.getName());

            String address = getEndpointAddress(exchange);
            String transactionName = (address != null) ? CxfInvokerUtils.getCXFRequestUri(address, method) :
                    CxfInvokerUtils.buildCXFTransactionName(serviceObject.getClass().getName(), method.getName());

            NewRelic.getAgent().getLogger().log(Level.FINEST,
                    "AbstractInvoker_Instrumentation: calling transaction.setTransactionName to [{0}] with priority FRAMEWORK_HIGH",
                    transactionName);
            transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "CXF", transactionName);
        }

        return Weaver.callOriginal();
    }

    private static String getEndpointAddress(Exchange exchange) {
        Endpoint endpoint = exchange.getEndpoint();
        if (endpoint == null) {
            return null;
        }

        EndpointInfo endpointInfo = endpoint.getEndpointInfo();
        return endpointInfo == null ? null : endpointInfo.getAddress();
    }
}
