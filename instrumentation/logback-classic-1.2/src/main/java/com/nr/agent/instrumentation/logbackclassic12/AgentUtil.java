package com.nr.agent.instrumentation.logbackclassic12;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import java.util.HashMap;
import java.util.Map;

import static com.nr.agent.instrumentation.logbackclassic12.ElementName.CLASS_NAME;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.ERROR_CLASS;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.ERROR_MESSAGE;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.ERROR_STACK;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.LOGGER_NAME;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.LOG_LEVEL;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.MESSAGE;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.THROWABLE;
import static com.nr.agent.instrumentation.logbackclassic12.ElementName.TIMESTAMP;

public class AgentUtil {

    public static void recordNewRelicLogEvent(String message, long timeStampMillis, Level level, Logger logger, String fqcn, Throwable throwable) {
        Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();
        HashMap<String, Object> logEventMap = new HashMap<>(agentLinkingMetadata);

        logEventMap.put(MESSAGE, message);
        logEventMap.put(TIMESTAMP, timeStampMillis);
        logEventMap.put(LOG_LEVEL, level);
        logEventMap.put(LOGGER_NAME, logger.getName());
        logEventMap.put(CLASS_NAME, fqcn);

        if (throwable != null) {
            logEventMap.put(THROWABLE, throwable.toString());
            logEventMap.put(ERROR_CLASS, throwable.getClass().getName());
            logEventMap.put(ERROR_MESSAGE, throwable.getMessage());
            logEventMap.put(ERROR_STACK, ExceptionUtil.getErrorStack(throwable));
        }

        AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
    }

    public static Map<String, String> getLinkingMetadataAsMap() {
        return NewRelic.getAgent().getLinkingMetadata();
    }

    public static String getLinkingMetadataAsString() {
        // TODO might need to filter the map entries to remove some (e.g. entity.*) and/or create a differently formatted string
        return NewRelic.getAgent().getLinkingMetadata().toString();
    }

    public static boolean isApplicationLoggingEnabled() {
        Object configValue = NewRelic.getAgent().getConfig().getValue("application_logging.enabled");
        // Config value is parsed as a String if it was set by system property or environment variable
        if (configValue instanceof String) {
            return Boolean.parseBoolean((String) configValue);
        }
        return (Boolean) configValue;
    }

    public static boolean isApplicationLoggingMetricsEnabled() {
        Object configValue = NewRelic.getAgent().getConfig().getValue("application_logging.metrics.enabled");
        // Config value is parsed as a String if it was set by system property or environment variable
        if (configValue instanceof String) {
            return Boolean.parseBoolean((String) configValue);
        }
        return (Boolean) configValue;
    }

    public static boolean isApplicationLoggingForwardingEnabled() {
        Object configValue = NewRelic.getAgent().getConfig().getValue("application_logging.forwarding.enabled");
        // Config value is parsed as a String if it was set by system property or environment variable
        if (configValue instanceof String) {
            return Boolean.parseBoolean((String) configValue);
        }
        return (Boolean) configValue;
    }

    public static boolean isApplicationLoggingLocalDecoratingEnabled() {
        Object configValue = NewRelic.getAgent().getConfig().getValue("application_logging.local_decorating.enabled");
        // Config value is parsed as a String if it was set by system property or environment variable
        if (configValue instanceof String) {
            return Boolean.parseBoolean((String) configValue);
        }
        return (Boolean) configValue;
    }
}
