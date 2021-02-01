package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.trace.v1.V1;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpanConverterTest {

    @Test
    void convert_Valid() throws IOException {
        SpanEvent spanEvent = buildSpanEvent();

        V1.Span result = SpanConverter.convert(spanEvent);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.writeTo(baos);
        V1.Span deserialized = V1.Span.parseFrom(baos.toByteArray());

        assertEquals("abc123", deserialized.getTraceId());
        assertEquals("abc123", deserialized.getIntrinsicsOrThrow("traceId").getStringValue());
        assertEquals("my app", deserialized.getIntrinsicsOrThrow("appName").getStringValue());
        assertEquals("value", deserialized.getIntrinsicsOrThrow("intrStr").getStringValue());
        assertEquals(12345, deserialized.getIntrinsicsOrThrow("intrInt").getIntValue());
        assertEquals(3.14, deserialized.getIntrinsicsOrThrow("intrFloat").getDoubleValue(), 0.00001);
        assertTrue(deserialized.getIntrinsicsOrThrow("intrBool").getBoolValue());
        assertFalse(deserialized.containsIntrinsics("intrOther"));
    }

    static SpanEvent buildSpanEvent() {
        return SpanEvent.builder()
                .appName("my app")
                .putIntrinsic("traceId", "abc123")
                .putIntrinsic("intrStr", "value")
                .putIntrinsic("intrInt", 12345)
                .putIntrinsic("intrFloat", 3.14)
                .putIntrinsic("intrBool", true)
                .putIntrinsic("intrOther", TestEnum.ONE)
                .build();
    }

    private enum TestEnum { ONE }

}
