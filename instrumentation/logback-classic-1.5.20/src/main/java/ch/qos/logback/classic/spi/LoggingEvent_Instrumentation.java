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

import java.util.Collections;
import java.util.Map;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isAppLoggingContextDataEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingForwardingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;
import static com.nr.agent.instrumentation.logbackclassic1520.AgentUtil.recordNewRelicLogEvent;

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
            this.message = message + getLinkingMetadataBlob();
        } else {
            this.message = message;
        }

        this.argumentArray = argArray;

        if (throwable == null) {
            throwable = extractThrowableAndRearrangeArguments(argArray);
        }

        if (throwable != null) {
            this.throwableProxy = new ThrowableProxy(throwable);
            LoggerContext lc = logger.getLoggerContext();
            if (lc.isPackagingDataEnabled()) {
                this.throwableProxy.calculatePackagingData();
            }
        }

        timeStamp = System.currentTimeMillis();

        Thread thread = Thread.currentThread();
        String threadName = thread.getName();
        long threadId = thread.getId();

        if (applicationLoggingEnabled && isApplicationLoggingForwardingEnabled()) {
            Map<String, String> mdc;
            if (isAppLoggingContextDataEnabled()) {
                mdc = getMdc();
            } else {
                mdc = Collections.emptyMap();
            }
            // Record and send LogEvent to New Relic
            recordNewRelicLogEvent(getFormattedMessage(), mdc, timeStamp, level, throwable, threadName, threadId, loggerName, fqnOfLoggerClass);
        }
    }

    public String getFormattedMessage() {
        return Weaver.callOriginal();
    }

    private Throwable extractThrowableAndRearrangeArguments(Object[] argArray) {
        return Weaver.callOriginal();
    }

    public Map<String, String> getMdc() {
        return Weaver.callOriginal();
    }

}
