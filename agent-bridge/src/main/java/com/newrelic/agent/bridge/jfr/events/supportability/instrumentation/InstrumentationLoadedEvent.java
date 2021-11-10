package com.newrelic.agent.bridge.jfr.events.supportability.instrumentation;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(InstrumentationLoadedEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Instrumentation" })
@Label("Loaded Modules")
@Description("Supportability info")
@StackTrace(false)
public class InstrumentationLoadedEvent extends Event {
    static final String NAME = "com.newrelic.InstrumentationLoaded";

    @Label("Custom")
    public boolean custom;

    @Label("Classloader")
    public String classloader;

    @Label("Weave Package Name")
    public String weavePackageName;

    @Label("Weave Package Version")
    public float weavePackageVersion;
}
