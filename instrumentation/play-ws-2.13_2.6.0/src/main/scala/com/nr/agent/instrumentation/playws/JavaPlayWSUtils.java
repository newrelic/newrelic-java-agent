/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.playws;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.StandaloneWSResponse;

import java.net.URL;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

public class JavaPlayWSUtils {

    public static Segment start() {
        Segment segment = null;
        try {
            segment = NewRelic.getAgent().getTransaction().startSegment("External", "PlayWS");
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
            try {
                if (segment != null) {
                    segment.end();
                    segment = null;
                }
            } catch (Throwable t1) {
                AgentBridge.instrumentation.noticeInstrumentationError(t1, Weaver.getImplementationTitle());
            }
        }
        return segment;
    }

    public static CompletionStage<? extends StandaloneWSResponse> finish(Segment segment, String procedure, StandaloneWSRequest request,
            CompletionStage<? extends StandaloneWSResponse> response) {
        if (segment == null) {
            return response;
        }

        final Segment localSegment = segment;
        localSegment.setMetricName("External", "PlayWS", procedure);

        try {
            response = response.whenComplete(new BiConsumer<StandaloneWSResponse, Throwable>() {
                @Override
                public void accept(StandaloneWSResponse standaloneWSResponse, Throwable throwable) {
                    if (throwable != null) {
                        try {
                            localSegment.reportAsExternal(HttpParameters
                                    .library("PlayWS")
                                    .uri(new URL(request.getUrl()).toURI())
                                    .procedure(procedure)
                                    .noInboundHeaders()
                                    .build());
                            localSegment.end();
                        } catch (Exception e) {
                        }
                    } else {
                        try {
                            localSegment.reportAsExternal(HttpParameters
                                    .library("PlayWS")
                                    .uri(new URL(request.getUrl()).toURI())
                                    .procedure(procedure)
                                    .inboundHeaders(new JavaInboundWrapper(standaloneWSResponse))
                                    .build());
                            localSegment.end();
                        } catch (Exception e) {
                        }
                    }
                }
            });
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
            try {
                localSegment.end();
            } catch (Throwable t1) {
                AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
            }
        }

        return response;
    }

}
