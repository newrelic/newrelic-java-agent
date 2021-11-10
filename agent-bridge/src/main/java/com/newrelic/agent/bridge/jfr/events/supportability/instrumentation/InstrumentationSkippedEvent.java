package com.newrelic.agent.bridge.jfr.events.supportability.instrumentation;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(InstrumentationSkippedEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Instrumentation" })
@Label("Skipped Modules")
@Description("Supportability info")
@StackTrace(false)
public class InstrumentationSkippedEvent extends Event {
    static final String NAME = "com.newrelic.InstrumentationSkipped";

    @Label("Custom")
    public boolean custom;

    @Label("Classloader")
    public String classloader;

    @Label("Weave Package Name")
    public String weavePackageName;

    @Label("Weave Package Version")
    public float weavePackageVersion;
}
