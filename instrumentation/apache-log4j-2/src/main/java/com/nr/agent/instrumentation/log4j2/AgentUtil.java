package com.nr.agent.instrumentation.log4j2;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

import java.util.HashMap;
import java.util.Map;

import static com.nr.agent.instrumentation.log4j2.ElementName.CLASS_NAME;
import static com.nr.agent.instrumentation.log4j2.ElementName.ERROR_CLASS;
import static com.nr.agent.instrumentation.log4j2.ElementName.ERROR_MESSAGE;
import static com.nr.agent.instrumentation.log4j2.ElementName.ERROR_STACK;
import static com.nr.agent.instrumentation.log4j2.ElementName.LOGGER_NAME;
import static com.nr.agent.instrumentation.log4j2.ElementName.LOG_LEVEL;
import static com.nr.agent.instrumentation.log4j2.ElementName.MESSAGE;
import static com.nr.agent.instrumentation.log4j2.ElementName.THROWABLE;
import static com.nr.agent.instrumentation.log4j2.ElementName.TIMESTAMP;

public class AgentUtil {

    public static void recordNewRelicLogEvent(LogEvent event) {
        final String EMPTY_STRING = "";

        if (event != null) {
            Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();
            HashMap<String, Object> logEventMap = new HashMap<>(agentLinkingMetadata);

            Message message = event.getMessage();
            logEventMap.put(MESSAGE, message != null ? message.getFormattedMessage() : EMPTY_STRING);
            logEventMap.put(TIMESTAMP, event.getTimeMillis());

            Level level = event.getLevel();
            logEventMap.put(LOG_LEVEL, level != null ? level.name() : EMPTY_STRING);
            logEventMap.put(LOGGER_NAME, event.getLoggerName());
            logEventMap.put(CLASS_NAME, event.getLoggerFqcn());

            Throwable throwable = event.getThrown();
            if (throwable != null) {
                logEventMap.put(THROWABLE, throwable.toString());
                logEventMap.put(ERROR_CLASS, throwable.getClass().getName());
                logEventMap.put(ERROR_MESSAGE, throwable.getMessage());
                logEventMap.put(ERROR_STACK, ExceptionUtil.getErrorStack(throwable));
            }

            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
        }
    }

    public static String getLinkingMetadataAsString() {
        // TODO might need to filter the map entries to remove some (e.g. entity.*) and/or create a differently formatted string
        return NewRelic.getAgent().getLinkingMetadata().toString();
    }

    public static Map<String, String> getLinkingMetadataAsMap() {
        return NewRelic.getAgent().getLinkingMetadata();
    }
}
