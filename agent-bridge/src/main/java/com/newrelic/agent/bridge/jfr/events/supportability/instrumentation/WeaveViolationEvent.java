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

@Name(WeaveViolationEvent.NAME)
@Category({ NEW_RELIC_JFR_EVENT, SUPPORTABILITY, INSTRUMENTATION })
@Label("Weave Violation")
@Description("Supportability info")
@StackTrace(false)
public class WeaveViolationEvent extends Event {
    static final String NAME = NR_NAMESPACE_PREFIX + "WeaveViolation";

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
