package com.newrelic.agent.bridge.jfr.events.supportability.instrumentation;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(WeaveClassEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Instrumentation" })
@Label("Weave Class")
@Description("Supportability info")
@StackTrace(false)
public class WeaveClassEvent extends Event {
    static final String NAME = "com.newrelic.WeaveClass";

    @Label("Weave Package")
    public String weavePackage;

    @Label("Weave Class")
    public String weaveClass;
}
