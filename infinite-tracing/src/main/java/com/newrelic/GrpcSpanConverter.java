package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.trace.v1.V1;

import java.util.HashMap;
import java.util.Map;

public class GrpcSpanConverter implements SpanConverter<V1.Span> {
    public V1.Span convert(SpanEvent spanEvent) {
        Map<String, V1.AttributeValue> intrinsicAttributes = copyAttributes(spanEvent.getIntrinsics());
        Map<String, V1.AttributeValue> userAttributes = copyAttributes(spanEvent.getUserAttributesCopy());
        Map<String, V1.AttributeValue> agentAttributes = copyAttributes(spanEvent.getAgentAttributes());

        intrinsicAttributes.put("appName", V1.AttributeValue.newBuilder().setStringValue(spanEvent.getAppName()).build());

        return V1.Span.newBuilder()
                .setTraceId(spanEvent.getTraceId())
                .putAllIntrinsics(intrinsicAttributes)
                .putAllAgentAttributes(agentAttributes)
                .putAllUserAttributes(userAttributes)
                .build();
    }

    private Map<String, V1.AttributeValue> copyAttributes(Map<String, Object> original) {
        Map<String, V1.AttributeValue> copy = new HashMap<>();
        if (original == null) {
            return copy;
        }

        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setStringValue((String) value).build());
            } else if (value instanceof Long || value instanceof Integer) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setIntValue(((Number) value).longValue()).build());
            } else if (value instanceof Float || value instanceof Double) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setDoubleValue(((Number) value).doubleValue()).build());
            } else if (value instanceof Boolean) {
                copy.put(entry.getKey(), V1.AttributeValue.newBuilder().setBoolValue((Boolean) value).build());
            }
        }
        return copy;
    }
}
