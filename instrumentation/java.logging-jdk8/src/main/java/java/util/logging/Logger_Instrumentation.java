/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.util.logging;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingForwardingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingMetricsEnabled;
import static com.nr.instrumentation.jul.AgentUtil.recordNewRelicLogEvent;

@Weave(originalName = "java.util.logging.Logger")
public class Logger_Instrumentation {
    @NewField
    public static AtomicBoolean instrumented = new AtomicBoolean(false);

    @WeaveAllConstructors
    Logger_Instrumentation() {
        // Generate the instrumentation module supportability metric only once
        if (!instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/JavaUtilLogging/enabled");
        }
    }

    // Get the current filter for this Logger.
    public Filter getFilter() {
        return Weaver.callOriginal();
    }

    // Check if a message of the given level would actually be logged by this logger.
    // This check is based on the Loggers effective level, which may be inherited from its parent.
    public boolean isLoggable(Level level) {
        return Boolean.TRUE.equals(Weaver.callOriginal());
    }

    public void log(LogRecord record) {
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
