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

@Name(WeaveClassEvent.NAME)
@Category({ NEW_RELIC_JFR_EVENT, SUPPORTABILITY, INSTRUMENTATION })
@Label("Weave Class")
@Description("Supportability info")
@StackTrace(false)
public class WeaveClassEvent extends Event {
    static final String NAME = NR_NAMESPACE_PREFIX + "WeaveClass";

    @Label("Weave Package")
    public String weavePackage;

    @Label("Weave Class")
    public String weaveClass;
}
