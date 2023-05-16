package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.trace.v1.V1;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class SpanConverter {

    private SpanConverter() {
    }

    /**
     * Convert the span event the equivalent gRPC span.
     *
     * @param spanEvent the span event
     * @return the gRPC span
     */
    static V1.Span convert(SpanEvent spanEvent) {
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

    /**
     * Convert the batch of span events to the equivalent gRPC spans.
     *
     * @param spanEvents the span event batch
     * @return the gRPC span batch
     */
    static V1.SpanBatch convert(Collection<SpanEvent> spanEvents) {
        return V1.SpanBatch.newBuilder()
                .addAllSpans(spanEvents.stream().map(SpanConverter::convert).collect(Collectors.toList()))
                .build();
    }

    private static Map<String, V1.AttributeValue> copyAttributes(Map<String, Object> original) {
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
