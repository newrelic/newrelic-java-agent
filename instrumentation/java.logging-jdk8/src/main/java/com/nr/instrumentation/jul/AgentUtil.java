package com.nr.instrumentation.jul;

import com.newrelic.api.agent.NewRelic;

import java.util.Map;

public class AgentUtil {

    // TODO figure out how to recordNewRelicLogEvent for JUL
//    public static void recordNewRelicLogEvent(LogEvent event) {
//        final String EMPTY_STRING = "";
//
//        if (event != null) {
//            Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//            HashMap<String, Object> logEventMap = new HashMap<>(agentLinkingMetadata);
//
//            Message message = event.getMessage();
//            logEventMap.put(MESSAGE, message != null ? message.getFormattedMessage() : EMPTY_STRING);
//            logEventMap.put(TIMESTAMP, event.getTimeMillis());
//
//            Level level = event.getLevel();
//            logEventMap.put(LOG_LEVEL, level != null ? level.name() : EMPTY_STRING);
//            logEventMap.put(LOGGER_NAME, event.getLoggerName());
//            logEventMap.put(CLASS_NAME, event.getLoggerFqcn());
//
//            Throwable throwable = event.getThrown();
//            if (throwable != null) {
//                logEventMap.put(THROWABLE, throwable.toString());
//                logEventMap.put(ERROR_CLASS, throwable.getClass().getName());
//                logEventMap.put(ERROR_MESSAGE, throwable.getMessage());
//                logEventMap.put(ERROR_STACK, ExceptionUtil.getErrorStack(throwable));
//            }
//
//            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
//        }
//    }

    public static String getLinkingMetadataAsString() {
        // TODO might need to filter the map entries to remove some (e.g. entity.*) and/or create a differently formatted string
        return NewRelic.getAgent().getLinkingMetadata().toString();
    }

    public static Map<String, String> getLinkingMetadataAsMap() {
        return NewRelic.getAgent().getLinkingMetadata();
    }

    public static boolean isApplicationLoggingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.enabled");
    }

    public static boolean isApplicationLoggingMetricsEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.metrics.enabled");
    }

    public static boolean isApplicationLoggingForwardingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.forwarding.enabled");
    }

    public static boolean isApplicationLoggingLocalDecoratingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("application_logging.local_decorating.enabled");
    }
}
