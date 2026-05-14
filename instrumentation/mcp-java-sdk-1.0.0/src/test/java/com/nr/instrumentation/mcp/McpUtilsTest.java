package com.nr.instrumentation.mcp;

import com.newrelic.api.agent.Segment;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static com.nr.instrumentation.mcp.McpUtils.extractScheme;
import static com.nr.instrumentation.mcp.McpUtils.startMcpSegment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class McpUtilsTest {

    @Test
    public void testSuccessfulSchemeExtraction() {
        String uri1 = "https://example.com";
        String uri2 = "file:///path/to/file";
        String uri3 = "custom-scheme://example.com";

        String scheme1 = extractScheme(uri1);
        String scheme2 = extractScheme(uri2);
        String scheme3 = extractScheme(uri3);


        assertEquals("https", scheme1);
        assertEquals("file", scheme2);
        assertEquals("custom-scheme", scheme3);
    }

    @Test
    public void testFailedSchemeExtraction() {
        String uri1 = "";
        String uri2 = "file///path/to/file";

        String scheme1 = extractScheme(uri1);
        String scheme2 = extractScheme(uri2);
        String scheme3 = extractScheme(null);

        assertEquals("resource", scheme1);
        assertEquals("resource", scheme2);
        assertEquals("resource", scheme3);
    }

    @Test
    public void testUnmodifiedMonoReturnedWhenSegmentIsNull() {
        Mono<String> originalMono = Mono.just("test");
        Mono<String> resultMono = McpUtils.endSegmentOnFinally(originalMono, null);

        assertEquals("test", resultMono.block());
    }

    @Test
    public void testNullReturnedWhenMonoIsNull() {
        Mono<String> originalMono = null;
        Segment segment = startMcpSegment("Llm/tool/MCP/call_tool", null);

        assertNull(McpUtils.endSegmentOnFinally(originalMono, segment));
    }

}
