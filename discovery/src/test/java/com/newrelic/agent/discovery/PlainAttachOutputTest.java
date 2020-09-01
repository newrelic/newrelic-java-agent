package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class PlainAttachOutputTest {
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private PlainAttachOutput output;

    
    @Before
    public void before() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        output = new PlainAttachOutput(new PrintStream(out), new PrintStream(err));
    }

    @Test
    public void printDiscovery() {
        output.listHeader();
        output.list("1", "test", "1.8", true);
        output.applicationInfo(new ApplicationContainerInfo("1", "Jetty",
                Arrays.asList("Test")));
        assertEquals("", err.toString());
        assertEquals("Java processes:\n" +
                "PID\tVM Version\tAttachable\tDisplay Name\tServer Info\tApplication Names\n" +
                "1\t1.8\ttrue\ttest\tJetty\t[Test]\n", out.toString());
    }

    @Test
    public void printDiscoveryNoAttachCallback() {
        output.listHeader();
        output.list("1", "test", "1.8", true);
        output.close();
        assertEquals("", err.toString());
        assertEquals("Java processes:\n" +
                "PID\tVM Version\tAttachable\tDisplay Name\tServer Info\tApplication Names\n" +
                "1\t1.8\ttrue\ttest\n", out.toString());
    }

    @Test
    public void printDiscoveryNonAttachable() {
        output.listHeader();
        output.list("1", "test", "1.8", false);
        assertEquals("", err.toString());
        assertEquals("Java processes:\n" +
                "PID\tVM Version\tAttachable\tDisplay Name\tServer Info\tApplication Names\n" +
                "1\t1.8\tfalse\ttest\n", out.toString());
    }

    @Test
    public void printError() {
        IllegalArgumentException exception = new IllegalArgumentException("test");
        exception.setStackTrace(new StackTraceElement[0]);
        output.error(exception);
        assertEquals("java.lang.IllegalArgumentException: test\n", err.toString());
        assertEquals("", out.toString());
    }
}
