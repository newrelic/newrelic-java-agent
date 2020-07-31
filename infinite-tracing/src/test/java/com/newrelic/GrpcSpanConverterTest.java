package com.newrelic;

import com.newrelic.agent.model.SpanEvent;
import com.newrelic.trace.v1.V1;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrpcSpanConverterTest {
    private enum TestEnum { ONE }

    @Test
    public void shouldSerializeIntrinsicAttributesOK() throws IOException {
        SpanEvent spanEvent = makeSpanWithIntrinsics();

        V1.Span deserialized = from(spanEvent);
        assertEquals("my app", deserialized.getIntrinsicsOrThrow("appName").getStringValue());
        assertEquals("value", deserialized.getIntrinsicsOrThrow("intrStr").getStringValue());
        assertEquals(12345, deserialized.getIntrinsicsOrThrow("intrInt").getIntValue());
        assertEquals(3.14, deserialized.getIntrinsicsOrThrow("intrFloat").getDoubleValue(), 0.00001);
        assertTrue(deserialized.getIntrinsicsOrThrow("intrBool").getBoolValue());

        assertFalse(deserialized.containsIntrinsics("intrOther"));
    }

    @Test
    public void shouldStoreTraceIdBothPlaces() throws IOException {
        SpanEvent spanEvent = makeSpanWithIntrinsics();

        V1.Span deserialized = from(spanEvent);
        assertEquals("abc123", deserialized.getTraceId());
        assertEquals("abc123", deserialized.getIntrinsicsOrThrow("traceId").getStringValue());
    }

    private V1.Span from(SpanEvent spanEvent) throws IOException {
        GrpcSpanConverter target = new GrpcSpanConverter();
        V1.Span result = target.convert(spanEvent);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.writeTo(baos);
        return V1.Span.parseFrom(baos.toByteArray());
    }

    private SpanEvent makeSpanWithIntrinsics() {
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
}
