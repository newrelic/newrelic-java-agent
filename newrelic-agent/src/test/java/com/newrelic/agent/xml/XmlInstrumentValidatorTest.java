package com.newrelic.agent.xml;

import static com.newrelic.agent.AgentHelper.getFile;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.text.MessageFormat;

import com.newrelic.agent.xml.XmlInstrumentOptions;
import com.newrelic.agent.xml.XmlInstrumentParams;
import com.newrelic.agent.xml.XmlInstrumentValidator;
import org.apache.commons.cli.CommandLine;
import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.agent.extension.util.XmlException;
import org.mockito.Mockito;

public class XmlInstrumentValidatorTest {

    /** Test case one. */
    private static final String FILE_PATH_1 = "com/newrelic/agent/command/commandLineExtension1.xml";
    /** Test file two. */
    private static final String FILE_PATH_2_INTERFACE = "com/newrelic/agent/command/commandLineExtension2.xml";
    /** Test file three. Contains a static method. */
    private static final String FILE_PATH_3_STATIC = "com/newrelic/agent/command/commandLineExtension3.xml";
    /** Test file four. Contains an innner class. */
    private static final String FILE_PATH_4_INNER_CLASS = "com/newrelic/agent/command/commandLineExtension4.xml";
    /** Test file five. Contains a static method and a constructor. */
    private static final String FILE_PATH_5_CONSTRUCTOR = "com/newrelic/agent/command/customLineExtension5.xml";
    /** Test file six. Contains duplicate point cuts. */
    private static final String FILE_PATH_6_DUPLICATES = "com/newrelic/agent/command/customLineExtension6Dups.xml";
    /** This is the file which gets put in the zip file. */
    private static final String FILE_ONE_IN_BUILD = "extension-example.xml";

    /**
     * This is the xml file which is distributed to customers. Make sure it passes!
     */
    @Test
    public void testValidateFileInDistributedZIP() {

        XmlInstrumentParams params = new XmlInstrumentParams();
        params.setDebug(new String[] { "true" }, XmlInstrumentOptions.DEBUG_FLAG.getFlagName());
        File theXml = getFile(FILE_ONE_IN_BUILD);

        // just verify that an exception does not get thrown
        try {
            ExtensionDomParser.readFile(theXml);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testValidateInstrumentationClasses() {

        XmlInstrumentParams params = new XmlInstrumentParams();
        params.setDebug(new String[] { "true" }, XmlInstrumentOptions.DEBUG_FLAG.getFlagName());
        File theXml = getFile(FILE_PATH_1);
        params.setFile(new String[] { theXml.getAbsolutePath() }, XmlInstrumentOptions.FILE_PATH.getFlagName());

        // just verify that an exception does not get thrown
        try {
            XmlInstrumentValidator.validateInstrumentation(params);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testValidateInstrumentationInterfaces() {

        XmlInstrumentParams params = new XmlInstrumentParams();
        params.setDebug(new String[] { "true" }, XmlInstrumentOptions.DEBUG_FLAG.getFlagName());
        File theXml = getFile(FILE_PATH_2_INTERFACE);
        params.setFile(new String[] { theXml.getAbsolutePath() }, XmlInstrumentOptions.FILE_PATH.getFlagName());

        try {
            XmlInstrumentValidator.validateInstrumentation(params);
            Assert.fail("An exception should have been thrown " + "since the xml contains an interface.");
        } catch (Exception e) {
            // should go into here
        }
    }

    @Test
    public void testValidateInstrumentationStatic() {

        XmlInstrumentParams params = new XmlInstrumentParams();
        File theXml = getFile(FILE_PATH_3_STATIC);
        params.setFile(new String[] { theXml.getAbsolutePath() }, XmlInstrumentOptions.FILE_PATH.getFlagName());

        // just verify that an exception does not get thrown
        try {
            XmlInstrumentValidator.validateInstrumentation(params);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testValidateInstrumentationInnerClass() {

        XmlInstrumentParams params = new XmlInstrumentParams();
        params.setDebug(new String[] { "true" }, XmlInstrumentOptions.DEBUG_FLAG.getFlagName());
        File theXml = getFile(FILE_PATH_4_INNER_CLASS);
        params.setFile(new String[] { theXml.getAbsolutePath() }, XmlInstrumentOptions.FILE_PATH.getFlagName());

        // just verify that an exception does not get thrown
        try {
            XmlInstrumentValidator.validateInstrumentation(params);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testValidateInstrumentationConstructor() {

        XmlInstrumentParams params = new XmlInstrumentParams();
        params.setDebug(new String[] { "true" }, XmlInstrumentOptions.DEBUG_FLAG.getFlagName());
        File theXml = getFile(FILE_PATH_5_CONSTRUCTOR);
        params.setFile(new String[] { theXml.getAbsolutePath() }, XmlInstrumentOptions.FILE_PATH.getFlagName());

        try {
            XmlInstrumentValidator.validateInstrumentation(params);
            Assert.fail("An exception should have been thrown " + "since the xml contains a constructor.");
        } catch (IllegalArgumentException e) {
            // should go into here
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = XmlException.class)
    public void testValidateInstrumentationDuplicates() throws Exception {

        XmlInstrumentParams params = new XmlInstrumentParams();
        params.setDebug(new String[] { "true" }, XmlInstrumentOptions.DEBUG_FLAG.getFlagName());
        File theXml = getFile(FILE_PATH_6_DUPLICATES);
        params.setFile(new String[] { theXml.getAbsolutePath() }, XmlInstrumentOptions.FILE_PATH.getFlagName());

        XmlInstrumentValidator.validateInstrumentation(params);
    }

    @Test
    public void validateInstrumentation_shouldFail_noCmdLineParams() {
        ConsoleSpy.interceptConsole();

        XmlInstrumentValidator.validateInstrumentation((CommandLine) null);
        Assert.assertTrue(ConsoleSpy.getInterceptedOutput().toString().contains("There were no command line parameters"));

        ConsoleSpy.resetConsole();
    }

    @Test
    public void validateInstrumentation_shouldFail_noFileInCmdLineParams() {
        ConsoleSpy.interceptConsole();

        CommandLine mockCmd = Mockito.mock(CommandLine.class);
        Mockito.when(mockCmd.getOptionValues(anyString())).thenReturn(null);

        XmlInstrumentValidator.validateInstrumentation(mockCmd);
        Assert.assertTrue(ConsoleSpy.getInterceptedOutput().toString().contains("FAIL: The command line parameters are invalid."));

        ConsoleSpy.resetConsole();
    }

    @Test
    public void validateInstrumentation_shouldPass_validCmdLineParams() {
        ConsoleSpy.interceptConsole();

        CommandLine mockCmd = Mockito.mock(CommandLine.class);
        File xmlFile = getFile(FILE_PATH_1);
        Mockito.when(mockCmd.getOptionValues("file")).thenReturn(new String[] {xmlFile.getAbsolutePath()});
        Mockito.when(mockCmd.getOptionValues("debug")).thenReturn(new String[] {"true"});

        XmlInstrumentValidator.validateInstrumentation(mockCmd);
        Assert.assertTrue(ConsoleSpy.getInterceptedOutput().toString().contains("PASS"));

        ConsoleSpy.resetConsole();
    }

    @Test
    public void validateInstrumentation_shouldCatch_Exceptions(){
        ConsoleSpy.interceptConsole();

        CommandLine mockCmd = Mockito.mock(CommandLine.class);
        File xmlFile = getFile(FILE_PATH_5_CONSTRUCTOR);
        Mockito.when(mockCmd.getOptionValues("file")).thenReturn(new String[] {xmlFile.getAbsolutePath()});

        XmlInstrumentValidator.validateInstrumentation(mockCmd);
        Assert.assertTrue(ConsoleSpy.getInterceptedOutput().toString().contains(MessageFormat.format("FAIL: The extension at {0} failed validation", xmlFile.getAbsolutePath())));

        ConsoleSpy.resetConsole();

    }

    private static class ConsoleSpy {
        private static final PrintStream ORIGINAL_SYSTEM_OUT = System.out;
        private static ByteArrayOutputStream interceptedOutput;

        private static void interceptConsole(){
            interceptedOutput = new ByteArrayOutputStream();
            System.setOut(new PrintStream(interceptedOutput));
        }
        private static ByteArrayOutputStream getInterceptedOutput(){return interceptedOutput; }
        private static void resetConsole(){
            System.setOut(ORIGINAL_SYSTEM_OUT);
            interceptedOutput = null;
        }
    }



}
