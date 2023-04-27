package com.nr.agent.instrumentation.httpclient50;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class InstrumentationUtils {

    @Trace(async = true)
    public static void linkToken (Token token) {
        if (token != null) token.link();
    }
}
