package com.nr.agent.instrumentation.log4j2;

import com.newrelic.api.agent.NewRelic;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;

import java.util.HashMap;
import java.util.Map;

public class AgentUtil {

    public static void reportNewRelicLogEvent(LogEvent event) {
        final String EMPTY_STRING = "";

        if (event != null) {
            Map<String, String> agentLinkingMetadata = NewRelic.getAgent().getLinkingMetadata();
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

            NewRelic.getAgent().getLogSender().recordCustomEvent("LogEvent", logEventMap);
        }
    }

//    public static Map<String, String> createLogEventAttributesMap(LogEvent event) {
//        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//        // TODO omit attributes that don't have values
//        //  {trace.id=, hostname=192.168.1.8, entity.type=SERVICE, entity.guid=MjIxMjg2NHxBUE18QVBQTElDQVRJT058MTY2NjIxNDQ3NQ, entity.name=SpringBoot PetClinic, span.id=}
//        return linkingMetadata;
//    }
//
//    public static Map<String, String> getLinkingMetadataAsMap() {
//        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//        // TODO omit attributes that don't have values
//        //  {trace.id=, hostname=192.168.1.8, entity.type=SERVICE, entity.guid=MjIxMjg2NHxBUE18QVBQTElDQVRJT058MTY2NjIxNDQ3NQ, entity.name=SpringBoot PetClinic, span.id=}
//        return linkingMetadata;
//    }
//
//    public static String getLinkingMetadataAsString() {
//        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();
//        // TODO omit attributes that don't have values
//        //  {trace.id=, hostname=192.168.1.8, entity.type=SERVICE, entity.guid=MjIxMjg2NHxBUE18QVBQTElDQVRJT058MTY2NjIxNDQ3NQ, entity.name=SpringBoot PetClinic, span.id=}
//        return linkingMetadata.toString();
//    }
}
