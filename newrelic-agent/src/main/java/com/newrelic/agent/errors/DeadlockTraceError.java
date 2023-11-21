/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.util.StackTraces;

import java.lang.management.ThreadInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DeadlockTraceError extends TracedError {
    private final String message;
    private final String exceptionClass;
    private final Map<String, StackTraceElement[]> stackTraces;

    private DeadlockTraceError(ErrorCollectorConfig errorCollectorConfig, String appName, String frontendMetricName,
            long timestampInMillis, String message, String exceptionClass, Map<String, StackTraceElement[]> stackTraces,
            Map<String, ?> errorAttributes) {
        super(errorCollectorConfig, appName, frontendMetricName, timestampInMillis, "", null, null, null,
                errorAttributes, null, null, false, null);
        this.stackTraces = stackTraces;
        this.message = message;
        this.exceptionClass = exceptionClass;
    }

    public static class Builder extends TracedError.Builder implements DeadlockTraceErrorRequired {

        private String message;
        private String exceptionClass;
        private Map<String, StackTraceElement[]> stackTraces;

        Builder(ErrorCollectorConfig errorCollectorConfig, String appName, long timestampInMillis) {
            super(errorCollectorConfig, appName, "Unknown", timestampInMillis);
        }

        @Override
        public Builder threadInfoAndStackTrace(ThreadInfo thread, Map<String, StackTraceElement[]> stackTraces) {
            this.message = "Deadlocked thread: " + thread.getThreadName();
            this.exceptionClass = "Deadlock";
            this.stackTraces = stackTraces;
            return this;
        }

        public DeadlockTraceError build() {
            return new DeadlockTraceError(errorCollectorConfig, appName, frontendMetricName, timestampInMillis, message,
                    exceptionClass, stackTraces, errorAttributes);
        }

    }

    public static DeadlockTraceErrorRequired builder(ErrorCollectorConfig errorCollectorConfig, String appName,
            long timestampInMillis) {
        return new Builder(errorCollectorConfig, appName, timestampInMillis);
    }

    public interface DeadlockTraceErrorRequired {

        Builder threadInfoAndStackTrace(ThreadInfo thread, Map<String, StackTraceElement[]> stackTraces);

    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getExceptionClass() {
        return exceptionClass;
    }

    @Override
    public Collection<String> stackTrace() {
        return null;
    }

    @Override
    public boolean incrementsErrorMetric() {
        return false;
    }

    @Override
    public Map<String, Collection<String>> stackTraces() {
        Map<String, Collection<String>> traces = new HashMap<>();
        for (Entry<String, StackTraceElement[]> entry : stackTraces.entrySet()) {
            traces.put(entry.getKey(), StackTraces.stackTracesToStrings(entry.getValue()));
        }
        return traces;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stackTraces == null) ? 0 : stackTraces.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeadlockTraceError other = (DeadlockTraceError) obj;
        if (stackTraces == null) {
            if (other.stackTraces != null) {
                return false;
            }
        } else if (!stackTraces.equals(other.stackTraces)) {
            return false;
        }
        return true;
    }
}
