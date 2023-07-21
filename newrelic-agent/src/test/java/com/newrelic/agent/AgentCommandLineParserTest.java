package com.newrelic.agent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AgentCommandLineParserTest {
    private final ByteArrayOutputStream replacementOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream replacementErr = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    /**
     * Replace stdout/stderr with a ByteArrayOutputStream to assert on command line parser output
     */
    @Before
    public void setup() {
        System.setOut(new PrintStream(replacementOut));
        System.setErr(new PrintStream(replacementErr));
    }

    @After
    public void cleanUp() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void parseCommand_withHelpOption_printsHelpText() {
        String[] args = new String[] { "-h" };
        AgentCommandLineParser parser = new AgentCommandLineParser();

        parser.parseCommand(args);

        Assert.assertTrue(replacementOut.toString().contains("usage: java -jar newrelic.jar"));
        Assert.assertTrue(replacementOut.toString().contains("Commands:"));
    }
}