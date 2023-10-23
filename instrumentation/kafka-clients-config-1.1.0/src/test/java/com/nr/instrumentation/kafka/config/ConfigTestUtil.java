package com.nr.instrumentation.kafka.config;

import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

abstract class ConfigTestUtil {

    static Logger mockLogger(final List<LoggedMessage> sink) {
        return Mockito.mock(Logger.class, invocation -> {
            if (invocation.getMethod().getName().equals("log")) {
                final Level level = invocation.getArgument(0);
                final String pattern;
                if (invocation.getArgument(1) instanceof String) {
                    pattern = invocation.getArgument(1);
                } else {
                    // 2nd argument must have been a Throwable, so the 3rd must be the pattern
                    pattern = invocation.getArgument(2);
                }
                sink.add(new LoggedMessage(level, pattern));
            }
            return null;
        });
    }

    static Insights customEventsSink(final List<SubmittedEvent> sink) {
        return (eventType, attributes) -> sink.add(new SubmittedEvent(System.currentTimeMillis(), eventType, attributes));
    }


    public static class LoggedMessage {

        public final Level level;
        public final String pattern;

        public LoggedMessage(final Level level, final String pattern) {
            this.level = level;
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return level + ":" + pattern;
        }
    }

    static class SubmittedEvent {

        public final long submissionTime;
        public final String eventType;
        public final Map<String, ?> attributes;

        public SubmittedEvent(final long submissionTime, final String eventType, final Map<String, ?> attributes) {
            this.submissionTime = submissionTime;
            this.eventType = eventType;
            this.attributes = attributes;
        }

        public boolean hasAttribute(final String attrName, final Object val) {
            return attributes.containsKey(attrName) && val.equals(attributes.get(attrName));
        }

        public boolean isClientConfig(final String eventType, final String clientId) {
            return eventType.equals(this.eventType) && hasAttribute("clientId", clientId);
        }
    }
}
