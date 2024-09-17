/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.boot.actuate.endpoint.web.servlet;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.actuator.SpringActuatorUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.logging.Level;

@Weave(type = MatchType.BaseClass, originalName = "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping")
public class AbstractWebMvcEndpointHandlerMapping_Instrumentation {
    @Weave(type = MatchType.ExactClass, originalName = "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping$OperationHandler")
    private static final class OperationHandler_Instrumentation {
        Object handle(HttpServletRequest request, Map<String, String> body) {
            Transaction transaction = NewRelic.getAgent().getTransaction();

            if (transaction != null) {
                NewRelic.getAgent().getLogger().log(Level.INFO, "DUF- txn exists");
                String uri = SpringActuatorUtils.normalizeActuatorUri(request.getRequestURI());
                String reportablePrefix = SpringActuatorUtils.getReportableUriFromActuatorEndpoint(uri);

                NewRelic.getAgent().getLogger().log(Level.INFO, "DUF- uri: {0}   prefix: {1}     {2}", uri, reportablePrefix, request.getRequestURI());

                if (reportablePrefix != null) {
                    transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Spring",
                            reportablePrefix + " (" + request.getMethod() + ")");
                }
            }

            return Weaver.callOriginal();
        }
    }
}
