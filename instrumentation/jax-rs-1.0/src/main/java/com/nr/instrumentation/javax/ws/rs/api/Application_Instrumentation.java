package com.nr.instrumentation.javax.ws.rs.api;

import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "javax.ws.rs.core.Application")
public class Application_Instrumentation {
    // putting this here to prevent 3.0 implementations to be weaved by this module
}
