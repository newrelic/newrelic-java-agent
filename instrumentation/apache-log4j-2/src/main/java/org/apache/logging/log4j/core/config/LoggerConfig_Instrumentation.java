package org.apache.logging.log4j.core.config;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.log4j2.AgentUtil;
import org.apache.logging.log4j.core.LogEvent;

@Weave(originalName = "org.apache.logging.log4j.core.config.LoggerConfig", type = MatchType.ExactClass)
public class LoggerConfig_Instrumentation {

    protected void callAppenders(LogEvent event) {
        // Generate log usage metrics
        NewRelic.incrementCounter("Logging/lines");
        NewRelic.incrementCounter("Logging/lines/" + event.getLevel().toString());

        // Record and send LogEvent to New Relic
        AgentUtil.recordNewRelicLogEvent(event);

        Weaver.callOriginal();
    }
}