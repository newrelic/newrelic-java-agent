package org.glassfish.main.jul;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingForwardingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingMetricsEnabled;
import static com.nr.instrumentation.glassfish.jul.AgentUtil.recordNewRelicLogEvent;
import static java.util.Objects.requireNonNull;

@Weave(type = MatchType.ExactClass, originalName = "org.glassfish.main.jul.GlassFishLogger")
public class GlassFishLogger_Instrumentation extends Logger {
    @NewField
    public static AtomicBoolean instrumented = new AtomicBoolean(false);

    protected GlassFishLogger_Instrumentation(final String name) {
        super(name, null);

        // Generate the instrumentation module supportability metric only once
        if (!instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/GlassFishJUL/enabled");
        }
    }

    GlassFishLogger_Instrumentation(final Logger logger) {
        // resource bundle name is taken from the set resource bundle
        super(requireNonNull(logger, "logger is null!").getName(), null);

        // Generate the instrumentation module supportability metric only once
        if (!instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/GlassFishJUL/enabled");
        }
    }

    // Check if a message of the given level would actually be logged by this logger.
    // This check is based on the Loggers effective level, which may be inherited from its parent.
    public boolean isLoggable(Level level) {
        return Boolean.TRUE.equals(Weaver.callOriginal());
    }

    void checkAndLog(LogRecord record) {
        // Do nothing if application_logging.enabled: false
        if (isApplicationLoggingEnabled()) {

            boolean shouldLog = isLoggable(record.getLevel()) && getFilter() == null || getFilter().isLoggable(record);
            if (isApplicationLoggingMetricsEnabled() && shouldLog) {
                // Generate log level metrics
                NewRelic.incrementCounter("Logging/lines");
                NewRelic.incrementCounter("Logging/lines/" + record.getLevel().toString());
            }
            if (isApplicationLoggingForwardingEnabled() && shouldLog) {
                // Record and send LogEvent to New Relic
                recordNewRelicLogEvent(record);
            }
        }
        Weaver.callOriginal();
    }
}
