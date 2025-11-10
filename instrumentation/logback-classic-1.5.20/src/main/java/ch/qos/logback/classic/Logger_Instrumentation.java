/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package ch.qos.logback.classic;

import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import org.slf4j.Marker;

import java.util.concurrent.atomic.AtomicBoolean;

@Weave(originalName = "ch.qos.logback.classic.Logger", type = MatchType.ExactClass)
public abstract class Logger_Instrumentation {
    @NewField
    public static AtomicBoolean instrumented = new AtomicBoolean(false);

    @WeaveAllConstructors
    Logger_Instrumentation() {
        // Generate the instrumentation module supportability metric only once
        if (!instrumented.getAndSet(true)) {
            NewRelic.incrementCounter("Supportability/Logging/Java/LogbackClassic1.2/enabled");
        }
    }

    private void buildLoggingEventAndAppend(final String localFQCN, final Marker marker, final Level level, final String msg, final Object[] params,
            final Throwable t) {
        // Do nothing if application_logging.enabled: false
        if (AppLoggingUtils.isApplicationLoggingEnabled()) {
            if (AppLoggingUtils.isApplicationLoggingMetricsEnabled()) {
                // Generate log level metrics
                NewRelic.incrementCounter("Logging/lines");
                NewRelic.incrementCounter("Logging/lines/" + level);
            }
        }
        Weaver.callOriginal();
    }
}
