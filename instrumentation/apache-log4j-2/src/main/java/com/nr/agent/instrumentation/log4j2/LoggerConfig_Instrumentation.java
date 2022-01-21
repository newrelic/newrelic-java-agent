package com.nr.agent.instrumentation.log4j2;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.logging.log4j.core.LogEvent;

@Weave(originalName = "org.apache.logging.log4j.core.config.LoggerConfig")
public class LoggerConfig_Instrumentation {

    protected void callAppenders(LogEvent event) {
        NewRelic.incrementCounter("Logging/lines");
        NewRelic.incrementCounter("Logging/lines/" + event.getLevel().toString());
        Weaver.callOriginal();
    }

}