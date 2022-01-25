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
        NewRelic.incrementCounter("Logging/lines");
        NewRelic.incrementCounter("Logging/lines/" + event.getLevel().toString());
        AgentUtil.reportNewRelicLogEvent(event);

        Weaver.callOriginal();
    }


//    private void processLogEvent(final LogEvent event, final LoggerConfig.LoggerConfigPredicate predicate) {
//        event.setIncludeLocation(isIncludeLocation());
//        if (predicate.allow(this)) {
//            callAppenders(event);
//        }
//        logParent(event, predicate);
//    }


}