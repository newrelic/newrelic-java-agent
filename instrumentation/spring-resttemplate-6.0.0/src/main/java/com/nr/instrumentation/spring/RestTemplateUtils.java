/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.spring;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class RestTemplateUtils {

    public static final String LIBRARY = "RestTemplate";
    public static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    public static void handleUnknownHost(Exception e) {
        NewRelic.getAgent().getLogger().log(Level.FINER, e, "Caught exception, checking for UnknownHost");
        if (e instanceof UnknownHostException || (e.getCause() instanceof UnknownHostException)) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure("execute")
                    .build());
        }
    }

    public static <T> void processResponse(URI uri, HttpMethod method, T result) {
        if (uri == null) {
            return;
        }

        try {
            String procedure = method != null ? method.name() : "execute";

            // Use HttpParameters to include full path in segment name
            NewRelic.getAgent().getTracedMethod().reportAsExternal(HttpParameters
                    .library(LIBRARY)
                    .uri(uri)
                    .procedure(procedure)
                    .noInboundHeaders()
                    .build());
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINE, e, "Error reporting external call");
        }
    }
}