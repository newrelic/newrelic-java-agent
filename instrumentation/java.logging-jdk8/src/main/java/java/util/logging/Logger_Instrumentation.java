/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package java.util.logging;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingMetricsEnabled;

@Weave(originalName = "java.util.logging.Logger")
public class Logger_Instrumentation {

    public Filter getFilter() {
        return Weaver.callOriginal();
    }

    public boolean isLoggable(Level level) {
        return Boolean.TRUE.equals(Weaver.callOriginal());
    }

    public void log(LogRecord record) {
        // Do nothing if application_logging.enabled: false
        if (isApplicationLoggingEnabled()) {
            if (isApplicationLoggingMetricsEnabled()) {
                if (isLoggable(record.getLevel()) && getFilter() == null || getFilter().isLoggable(record)) {
                    // Generate log level metrics
                    NewRelic.incrementCounter("Logging/lines");
                    NewRelic.incrementCounter("Logging/lines/" + record.getLevel().toString());
                }
            }
        }
        Weaver.callOriginal();
    }

}
