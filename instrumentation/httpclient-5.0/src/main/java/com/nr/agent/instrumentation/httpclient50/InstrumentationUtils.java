package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.NewRelic;
import org.apache.hc.core5.http.HttpRequest;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class InstrumentationUtils {

    public static final String LIBRARY = "CommonsHttp";
    public static final String PROCEDURE = "execute";
    public static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    // TODO remove
//    public static void doOutboundCATClassic(ClassicHttpRequest request) {
//        NewRelic.getAgent().getLogger().log(Level.INFO, "inside doOutboundCATClassic");
//        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(request));
//    }
//
    public static void doOutboundCAT(HttpRequest request) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "inside doOutboundCAT");
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(new OutboundWrapper(request));
    }

    public static void handleUnknownHost(Exception e) {
        if (e instanceof UnknownHostException) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(GenericParameters
                    .library(LIBRARY)
                    .uri(UNKNOWN_HOST_URI)
                    .procedure(PROCEDURE)
                    .build());
        }
    }
}
