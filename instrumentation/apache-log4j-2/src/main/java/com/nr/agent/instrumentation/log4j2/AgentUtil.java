package com.nr.agent.instrumentation.log4j2;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AgentUtil {

    public static void recordNewRelicLogEvent(LogEvent event) {
        final String EMPTY_STRING = "";

        if (event != null) {
//            Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();
            Map<String, String> agentLinkingMetadata = getNewRelicLinkingMetadata();
            HashMap<String, Object> logEventMap = new HashMap<>(agentLinkingMetadata);

            Message message = event.getMessage();
            logEventMap.put("message", message != null ? message.getFormattedMessage() : EMPTY_STRING);
            logEventMap.put("timeStampTimeMillis", event.getTimeMillis());

            Instant instant = event.getInstant();
            logEventMap.put("timeStampInstantEpochSecond", instant != null ? instant.getEpochSecond() : EMPTY_STRING);

            Level level = event.getLevel();
            logEventMap.put("log.level", level != null ? level.name() : EMPTY_STRING);
            logEventMap.put("logger.name", event.getLoggerName());
            logEventMap.put("class.name", event.getLoggerFqcn());

            Throwable throwable = event.getThrown();
            logEventMap.put("throwable", throwable != null ? throwable.toString() : EMPTY_STRING);

            AgentBridge.getAgent().getLogSender().recordLogEvent(logEventMap);
        }
    }
    // TODO look into org.apache.logging.log4j.core.impl.ThreadContextDataInjector
    //  LogEvent actually has the linking metadata stored in the contextData when logged in LoggerConfig
    //  Just need to get the layout to show that data programmatically?
    //  Maybe this: Programmatically Modifying the Current Configuration after Initialization
    //  https://logging.apache.org/log4j/2.x/manual/customconfig.html

    public static Map<String, String> getNewRelicLinkingMetadata() {
        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
        if (linkingMetadata != null && linkingMetadata.size() > 0) {
            Map<String, String> map = new HashMap<>();
            Set<Map.Entry<String, String>> linkingMetadataSet = linkingMetadata.entrySet();
            for (Map.Entry<String, String> entry : linkingMetadataSet) {
//                map.put(NEW_RELIC_PREFIX + entry.getKey(), entry.getValue());
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }
}
