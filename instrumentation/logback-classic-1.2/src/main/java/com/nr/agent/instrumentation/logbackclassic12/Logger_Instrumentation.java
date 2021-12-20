package com.nr.agent.instrumentation.logbackclassic12;

import ch.qos.logback.classic.Level;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.slf4j.Marker;

@Weave(originalName = "ch.qos.logback.classic.Logger", type = MatchType.Interface)
public abstract class Logger_Instrumentation {
    private static final String LOG_LINES_METRIC = "Logging/lines";
    private static final String LOG_LINES_BY_LEVEL_METRIC_TEMPLATE = "Logging/lines/%s";

    private void buildLoggingEventAndAppend(final String localFQCN, final Marker marker, final Level level, final String msg, final Object[] params,
            final Throwable t) {
        Weaver.callOriginal();
        NewRelic.incrementCounter(LOG_LINES_METRIC);
        NewRelic.incrementCounter(String.format(LOG_LINES_BY_LEVEL_METRIC_TEMPLATE, level));
    }
}
