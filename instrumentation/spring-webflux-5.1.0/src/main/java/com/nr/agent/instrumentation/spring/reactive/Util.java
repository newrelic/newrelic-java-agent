/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spring.reactive;

import com.newrelic.api.agent.Token;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.ServerRequest;

public class Util {
    public static final String NR_TXN_NAME = "newrelic-transaction-name";
    public static final String NR_TOKEN = "newrelic-token";

    public static RequestPredicate createRequestPredicate(final String name, final RequestPredicate originalRequestPredicate) {
        return new RequestPredicate() {
            @Override
            public boolean test(ServerRequest request) {
                final boolean matched = originalRequestPredicate.test(request);
                if (matched) {
                    Util.addPath(request, "QueryParameter/" + name);
                }
                return matched;
            }

            @Override
            public String toString() {
                return "";
            }
        };
    }

    public static RequestPredicate createPathExtensionPredicate(String extension, RequestPredicate originalRequestPredicate) {
        return new RequestPredicate() {
            @Override
            public boolean test(ServerRequest request) {
                final boolean matched = originalRequestPredicate.test(request);
                if (matched) {
                    Util.addPath(request, "PathExtension/" + extension);
                }
                return matched;
            }

            @Override
            public String toString() {
                return "";
            }
        };
    }


    public static void addPath(ServerRequest request, String name) {
        Token token = (Token) request.attributes().get(NR_TOKEN);
        if (token != null && !name.isEmpty()) {
            request.attributes().computeIfAbsent(NR_TXN_NAME, k -> "");
            String existingName = (String) request.attributes().get(NR_TXN_NAME);
            request.attributes().put(NR_TXN_NAME, existingName + name);
        }
    }
}
