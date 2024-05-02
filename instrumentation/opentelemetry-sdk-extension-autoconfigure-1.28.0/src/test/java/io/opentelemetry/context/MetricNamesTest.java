package io.opentelemetry.context;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;

public class MetricNamesTest {
    @Test
    public void testTrimClassName() {
        assertEquals("io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender$$Lambda$",
                MetricNames.trimClassName(
                        "io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender$$Lambda$1381/0x0000000840afa440"));
    }

    @Test
    public void getMetricName() {
        Callable<Void> callable = () -> {
            return null;
        };
        assertEquals("Java/io.opentelemetry.context.MetricNamesTest$$Lambda$.call",
                MetricNames.getMetricName(callable.getClass(), "call"));
    }
}