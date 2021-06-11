package com.nr.instrumentation.vertx;
import java.net.URI;
import java.net.URISyntaxException;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import io.vertx.core.http.impl.HttpClientResponseImpl;

public class VertxCoreUtil {

	public static boolean initialized = false;
	
	private VertxCoreUtil() {
    }

    public static final String VERTX_CLIENT = "Vertx-Client";
    public static final String END = "end";

    private static URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");


    public static void processResponse(Segment segment, HttpClientResponseImpl resp, String host, int port,
            String scheme) {
        try {
            URI uri = new URI(scheme, null, host, port, null, null, null);
            segment.reportAsExternal(HttpParameters.library(VERTX_CLIENT)
                                                   .uri(uri)
                                                   .procedure(END)
                                                   .inboundHeaders(new InboundWrapper(resp))
                                                   .build());
        } catch (URISyntaxException e) {
            AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
        }
    }

    public static void reportUnknownHost(Segment segment) {
            segment.reportAsExternal(GenericParameters.library(VERTX_CLIENT)
                                                      .uri(UNKNOWN_HOST_URI)
                                                      .procedure(END)
                                                      .build());
    }

}