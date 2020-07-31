/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.stats.StatsService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

abstract class ConnectCycleSuccessBaseClientWrapper implements HttpClientWrapper {
    @Override
    public ReadResult execute(Request request, ExecuteEventHandler eventHandler) {
        assertEquals(Verb.POST, request.getVerb());

        String method = parseMethodFromRequest(request);
        assertNotNull(method);

        Object firstParameter = decompressBodyOfRequest(request);
        assertNotNull(firstParameter);

        String newHost = "new_host.example.com";

        switch (method) {
            case CollectorMethods.PRECONNECT:
                return mapPreconnectRequest(request, (JSONObject) firstParameter, newHost);
            case CollectorMethods.CONNECT:
                return mapConnectRequest(request, (JSONObject) firstParameter);
            default:
                throw new AssertionError("unexpected method: " + method);
        }
    }

    protected abstract ReadResult mapConnectRequest(Request request, JSONObject firstParameter);

    protected abstract ReadResult mapPreconnectRequest(Request request, JSONObject postedData, String newHost);

    private Object decompressBodyOfRequest(Request request) {
        Object postedData;
        try {
            Reader reader = new InputStreamReader(
                    new GZIPInputStream(new ByteArrayInputStream(request.getData())),
                    StandardCharsets.UTF_8
            );

            JSONArray params = (JSONArray) new JSONParser().parse(reader);
            postedData = params.get(0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        return postedData;
    }

    private String parseMethodFromRequest(Request request) {
        String method = null;
        for (String paramAndValue : request.getURL().getQuery().split("&")) {
            if (paramAndValue.startsWith("method")) {
                method = paramAndValue.split("=")[1];
            }
        }
        return method;
    }

    @Override
    public void captureSupportabilityMetrics(StatsService statsService, String requestHost) {

    }

    @Override
    public void shutdown() {

    }
}
