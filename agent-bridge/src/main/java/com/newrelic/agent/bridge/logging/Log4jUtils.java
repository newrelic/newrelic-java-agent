package com.newrelic.agent.bridge.logging;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.NewField;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;

public class Log4jUtils {

    /**
     * Gets the agent linking metadata from a LogEvent from Log4j.
     * This method relies on reflection to search for a new field from our Log4j 2.11+ instrumentation.
     *
     * @param logEvent an instrumented LogEvent instance
     * @return an opaque map of strings to strings
     */
    public static Map<String, String> getLinkingMetadata(Object logEvent) {
        if (logEvent == null) {
            return null;
        }
        Class<?> c = logEvent.getClass();

        Field[] fieldList = c.getFields();
        for (Field field : fieldList) {
            // Check if agentLinkingMetadata exists in LogEvent (instrumented in instrumentation:apache-log4j-2.11 and above)
            if (field.getAnnotationsByType(NewField.class).length != 0 && field.getName().equals("agentLinkingMetadata")) {
                try {
                    return (Map<String, String>) field.get(logEvent);
                } catch (IllegalAccessException | ClassCastException e) {
                    AgentBridge.getAgent().getLogger().log(Level.FINEST,
                            "Exception from getting linking metadata from log4j's LogEvent instance {0}", logEvent, e);
                }
            }
        }
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "No linking metadata found from log4j's LogEvent instance {0}", logEvent);
        return null;
    }
}
