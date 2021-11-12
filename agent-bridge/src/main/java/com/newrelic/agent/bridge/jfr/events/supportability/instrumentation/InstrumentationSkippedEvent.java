package com.newrelic.agent.bridge.jfr.events.supportability.instrumentation;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.INSTRUMENTATION;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NEW_RELIC_JFR_EVENT;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.NR_NAMESPACE_PREFIX;
import static com.newrelic.agent.bridge.jfr.events.JfrCustomEventConstants.SUPPORTABILITY;

@Name(InstrumentationSkippedEvent.NAME)
@Category({ NEW_RELIC_JFR_EVENT, SUPPORTABILITY, INSTRUMENTATION })
@Label("Module Skipped")
@Description("Supportability info")
@StackTrace(false)
public class InstrumentationSkippedEvent extends Event {
    static final String NAME = NR_NAMESPACE_PREFIX + "InstrumentationSkipped";

    @Label("Custom")
    public boolean custom;

    @Label("Classloader")
    public String classloader;

    @Label("Weave Package Name")
    public String weavePackageName;

    @Label("Weave Package Version")
    public float weavePackageVersion;
}
