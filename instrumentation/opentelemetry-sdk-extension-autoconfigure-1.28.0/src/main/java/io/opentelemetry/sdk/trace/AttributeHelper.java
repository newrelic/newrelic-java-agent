package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import io.opentelemetry.api.common.AttributeKey;

class AttributeHelper {
    AttributeHelper() {}

    public static <T> void setAttribute(Agent agent, AttributeKey<T> key, T value) {
        switch (key.getType()) {
            case BOOLEAN:
                agent.getTracedMethod().addCustomAttribute(key.getKey(), (Boolean) value);
                break;
            case LONG:
            case DOUBLE:
                agent.getTracedMethod().addCustomAttribute(key.getKey(), (Number) value);
                break;
            case STRING:
                agent.getTracedMethod().addCustomAttribute(key.getKey(), (String) value);
                break;
        }
    }
}
