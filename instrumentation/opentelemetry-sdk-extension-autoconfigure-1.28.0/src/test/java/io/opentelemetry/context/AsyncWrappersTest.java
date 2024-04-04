package io.opentelemetry.context;

import junit.framework.TestCase;
import org.junit.Test;

public class AsyncWrappersTest extends TestCase {

    @Test
    public void testTrimClassName() {
        assertEquals("io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender$$Lambda$",
                AsyncWrappers.trimClassName(
                "io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender$$Lambda$1381/0x0000000840afa440"));
    }
}
