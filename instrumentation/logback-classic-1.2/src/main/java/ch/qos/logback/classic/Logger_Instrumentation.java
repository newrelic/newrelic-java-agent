package ch.qos.logback.classic;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.logbackclassic12.AgentUtil;
import org.slf4j.Marker;

@Weave(originalName = "ch.qos.logback.classic.Logger", type = MatchType.ExactClass)
public abstract class Logger_Instrumentation {

    private void buildLoggingEventAndAppend(final String localFQCN, final Marker marker, final Level level, final String msg, final Object[] params,
            final Throwable t) {
        // Do nothing if application_logging.enabled: false
        if (AgentUtil.isApplicationLoggingEnabled()) {
            if (AgentUtil.isApplicationLoggingMetricsEnabled()) {
                // Generate log level metrics
                NewRelic.incrementCounter("Logging/lines");
                NewRelic.incrementCounter("Logging/lines/" + level);
            }
        }
        Weaver.callOriginal();
    }
}
