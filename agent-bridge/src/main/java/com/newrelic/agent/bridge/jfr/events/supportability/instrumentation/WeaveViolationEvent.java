package com.newrelic.agent.bridge.jfr.events.supportability.instrumentation;


import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(WeaveViolationEvent.NAME)
@Category({ "New Relic JFR Event", "Supportability", "Instrumentation" })
@Label("Weave Violation")
@Description("Supportability info")
@StackTrace(false)
public class WeaveViolationEvent extends Event {
    static final String NAME = "com.newrelic.WeaveViolation";

    @Label("Custom")
    public boolean custom;

    @Label("Weave Package")
    public String weavePackage;

    @Label("Weave Violation Size")
    public int weaveViolationSize;

    @Label("Classloader")
    public String classloader;

    @Label("Weave Violation Name")
    public String weaveViolationName;

    @Label("Weave Violation Class")
    public String weaveViolationClass;

    @Label("Weave Violation Method")
    public String weaveViolationMethod;

    @Label("Weave Violation Field")
    public String weaveViolationField;

    @Label("Weave Violation Reason")
    public String weaveViolationReason;
}
