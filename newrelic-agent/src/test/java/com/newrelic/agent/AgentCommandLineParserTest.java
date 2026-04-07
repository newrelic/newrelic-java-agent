package com.newrelic.agent;

import com.newrelic.agent.xml.XmlInstrumentValidator;
import com.newrelic.weave.verification.WeavePackageVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
        String[] args = new String[] { "-h"};
        AgentCommandLineParser parser = new AgentCommandLineParser();

        parser.parseCommand(args);

        Assert.assertTrue(replacementOut.toString().contains("usage: java -jar newrelic.jar"));
        Assert.assertTrue(replacementOut.toString().contains("Commands:"));
    }

    @Test
    public void parseCommand_withHelpOptionAndCommand_printsHelpText() {
        String[] args = new String[] { "-h", "instrument" };
        AgentCommandLineParser parser = new AgentCommandLineParser();

        parser.parseCommand(args);

        Assert.assertTrue(replacementOut.toString().contains("usage: java -jar newrelic.jar"));
        Assert.assertTrue(replacementOut.toString().contains("java -jar newrelic.jar"));
    }

    @Test
    public void parseCommand_withInstrumentOption_callsValidateInstrumentation() {
        String[] args = new String[] { "instrument" };
        AgentCommandLineParser parser = new AgentCommandLineParser();

        try (MockedStatic<XmlInstrumentValidator> validatorMock = Mockito.mockStatic(XmlInstrumentValidator.class)) {
            parser.parseCommand(args);
            validatorMock.verify(() -> XmlInstrumentValidator.validateInstrumentation(Mockito.any()));
        }
    }

    @Test
    public void parseCommand_withVerifyInstrumentationOption_callsVerifier() {
        String[] args = new String[] { "verifyInstrumentation" };
        AgentCommandLineParser parser = new AgentCommandLineParser();

        try (MockedStatic<WeavePackageVerifier> validatorMock = Mockito.mockStatic(WeavePackageVerifier.class)) {
            parser.parseCommand(args);
            validatorMock.verify(() -> WeavePackageVerifier.main(Mockito.any()));
        }
    }

    @Test
    public void parseCommand_withVersionOption_printsVersionText() {
        String[] args = new String[] { "-v" };
        AgentCommandLineParser parser = new AgentCommandLineParser();

        parser.parseCommand(args);

        Assert.assertNotNull(replacementOut.toString());
        Assert.assertFalse(replacementOut.toString().isEmpty());
    }
}