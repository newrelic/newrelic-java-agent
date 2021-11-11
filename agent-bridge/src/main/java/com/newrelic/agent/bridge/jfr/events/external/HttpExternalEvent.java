package com.newrelic.agent.bridge.jfr.events.external;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(HttpExternalEvent.NAME)
@Category({ "New Relic JFR Event", "External" })
@Label("Http External")
@Description("Http external call info")
@StackTrace(true)
public class HttpExternalEvent extends Event {
    static final String NAME = "com.newrelic.HttpExternal";

    @Label("HTTP Client")
    public String httpClient;

    @Label("Instrumentation")
    public String instrumentation;

    @Label("Error")
    public String error;

    @Label("Resource Method")
    public String method;

    @Label("Media Type")
    public String mediaType;

    @Label("Java Method")
    public String javaMethod;

    @Label("Path")
    public String path;

    @Label("Query Parameters")
    public String queryParameters;

    @Label("Headers")
    public String headers;

    @Label("Length")
    @DataAmount
    public int length;

    @Label("Response Headers")
    public String responseHeaders;

    @Label("Response Length")
    public int responseLength;

    @Label("Response Status")
    public int status;
}
