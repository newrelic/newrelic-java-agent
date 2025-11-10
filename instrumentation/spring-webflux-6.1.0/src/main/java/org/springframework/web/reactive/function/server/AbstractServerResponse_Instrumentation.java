/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive.function.server;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;

@Weave(type = MatchType.BaseClass, originalName = "org.springframework.web.reactive.function.server.DefaultServerResponseBuilder$AbstractServerResponse")
abstract class AbstractServerResponse_Instrumentation {

    public Mono<Void> writeTo(ServerWebExchange exchange, ServerResponse.Context context) {
        final Token token = (Token) exchange.getAttribute(Util.NR_TOKEN);
        if (token != null) {
            final Object pathPattern = exchange.getAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
            String txnName = exchange.getAttribute(Util.NR_TXN_NAME);
            if (pathPattern != null) {
                // If the pattern string provided by Spring is available we should use it
                if (pathPattern instanceof PathPattern) {
                    txnName = ((PathPattern) pathPattern).getPatternString();
                } else if (pathPattern instanceof String) {
                    txnName = (String) pathPattern;
                }
            }

            final String methodName = " (" + exchange.getRequest().getMethod() + ")";

            if (txnName != null && statusCode().value() != 404) {
                final String txnNameWithMethod = removeTrailingSlash(txnName) + methodName;
                token.getTransaction()
                        .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Spring", txnNameWithMethod);
            } else {
                token.getTransaction()
                        .setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, "Spring", "Unknown Route" + methodName);
            }
        }

        return Weaver.callOriginal();
    }

    public final HttpStatusCode statusCode() {
        return Weaver.callOriginal();
    }

    private String removeTrailingSlash(String txnName) {
        if (txnName.endsWith("/")) {
            return txnName.substring(0, txnName.length() - 1);
        }
        return txnName;
    }
}
