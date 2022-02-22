/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package ch.qos.logback.classic.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.getFilteredLinkingMetadataString;
import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.isApplicationLoggingEnabled;
import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.isApplicationLoggingForwardingEnabled;
import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.isApplicationLoggingLocalDecoratingEnabled;
import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.recordNewRelicLogEvent;

@Weave(originalName = "ch.qos.logback.classic.spi.LoggingEvent", type = MatchType.ExactClass)
public class LoggingEvent_Instrumentation {

    transient String fqnOfLoggerClass;
    private String loggerName;
    private LoggerContext loggerContext;
    private LoggerContextVO loggerContextVO;
    private transient Level level;
    private String message;
    private transient Object[] argumentArray;
    private ThrowableProxy throwableProxy;
    private long timeStamp;

    public LoggingEvent_Instrumentation(String fqcn, Logger logger, Level level, String message, Throwable throwable, Object[] argArray) {
        // Do nothing if application_logging.enabled: false
        this.fqnOfLoggerClass = fqcn;
        this.loggerName = logger.getName();
        this.loggerContext = logger.getLoggerContext();
        this.loggerContextVO = loggerContext.getLoggerContextRemoteView();
        this.level = level;

        boolean applicationLoggingEnabled = isApplicationLoggingEnabled();
        if (applicationLoggingEnabled && isApplicationLoggingLocalDecoratingEnabled()) {
            // Append New Relic linking metadata from agent to log message
            this.message = message + " NR-LINKING-METADATA: " + getFilteredLinkingMetadataString();
        } else {
            this.message = message;
        }

        this.argumentArray = argArray;

        if (throwable == null) {
            throwable = extractThrowableAnRearrangeArguments(argArray);
        }

        if (throwable != null) {
            this.throwableProxy = new ThrowableProxy(throwable);
            LoggerContext lc = logger.getLoggerContext();
            if (lc.isPackagingDataEnabled()) {
                this.throwableProxy.calculatePackagingData();
            }
        }

        timeStamp = System.currentTimeMillis();

        if (applicationLoggingEnabled && isApplicationLoggingForwardingEnabled()) {
            // Record and send LogEvent to New Relic
            recordNewRelicLogEvent(message, timeStamp, level);
        }
    }

    private Throwable extractThrowableAnRearrangeArguments(Object[] argArray) {
        return Weaver.callOriginal();
    }

}
