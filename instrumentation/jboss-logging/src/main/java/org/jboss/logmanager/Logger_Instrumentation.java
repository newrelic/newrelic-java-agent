/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.logmanager;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingForwardingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingMetricsEnabled;
import static com.nr.instrumentation.jboss.AgentUtil.recordNewRelicLogEvent;

@Weave(originalName = "org.jboss.logmanager.Logger")
public class Logger_Instrumentation {

    @NewField
    public static AtomicBoolean instrumented = new AtomicBoolean(false);

    @WeaveAllConstructors
    Logger_Instrumentation() {
        // Generate the instrumentation module supportability metric only once
        if (!instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/JBossLogging/enabled");
        }
    }

    public void logRaw(final ExtLogRecord record) {
        Weaver.callOriginal();

        if (isApplicationLoggingEnabled()) {
//            boolean shouldLog = isLoggable(record.getLevel()) && getFilter() == null || getFilter().isLoggable(record);
            if (isApplicationLoggingMetricsEnabled()) {
                // Generate log level metrics
                NewRelic.incrementCounter("Logging/lines");
                NewRelic.incrementCounter("Logging/lines/" + record.getLevel().toString());
            }
            if (isApplicationLoggingForwardingEnabled()) {
                // Record and send LogEvent to New Relic
                recordNewRelicLogEvent(record);
            }
        }
    }
}
