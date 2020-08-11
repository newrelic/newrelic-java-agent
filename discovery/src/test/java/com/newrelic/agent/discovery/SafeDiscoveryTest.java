package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;

import org.junit.Test;

public class SafeDiscoveryTest {

    public void checkToolsJar() {
        SafeDiscovery.checkToolsJar();
    }

    @Test
    public void printHelp() throws URISyntaxException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        try (PrintStream outStream = new PrintStream(out)) {
            try (PrintStream errStream = new PrintStream(err)) {
                SafeDiscovery.printHelp(outStream, errStream);
            }
        }

        assertTrue(out.toString().matches(
                "\\sjava -cp (.*):\\$\\{JAVA_HOME\\}/lib/tools.jar com.newrelic.bootstrap.BootstrapAgent attach\n"));
        assertEquals("Run this command using a Java 8 JDK with tools.jar on the classpath\n", err.toString());
    }
}
