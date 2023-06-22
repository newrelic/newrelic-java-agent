package com.newrelic.agent.xml;

import static com.newrelic.agent.AgentHelper.getFile;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

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
    public void validateInstrumentation_shouldHandle_noCmdLineParams() {
        final PrintStream out = System.out;
        final ByteArrayOutputStream interceptOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(interceptOut));

        CommandLine cmd = null;
        XmlInstrumentValidator.validateInstrumentation(cmd);

        Assert.assertEquals("There were no command line parameters.\n", interceptOut.toString());
        System.setOut(out);
    }

    @Test
    public void validateInstrumentation_shouldRequireFile_inCmdLineParams() {
        final PrintStream out = System.out;
        final ByteArrayOutputStream interceptOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(interceptOut));
        CommandLine mockCmd = Mockito.mock(CommandLine.class);
        Mockito.when(mockCmd.getOptionValues(anyString())).thenReturn(null);

        XmlInstrumentValidator.validateInstrumentation(mockCmd);

        Assert.assertTrue(interceptOut.toString().contains("FAIL: The command line parameters are invalid."));

        System.setOut(out);
    }

    @Test
    public void validateInstrumentation_shouldHandle_validCmdLineParams() {
        final PrintStream out = System.out;
        final ByteArrayOutputStream interceptOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(interceptOut));

        CommandLine mockCmd = Mockito.mock(CommandLine.class);
        File xmlFile = getFile(FILE_PATH_1);
        Mockito.when(mockCmd.getOptionValues("file")).thenReturn(new String[] {xmlFile.getAbsolutePath()});
        Mockito.when(mockCmd.getOptionValues("debug")).thenReturn(new String[] {"true"});

        XmlInstrumentValidator.validateInstrumentation(mockCmd);
        Assert.assertTrue(interceptOut.toString().contains("PASS"));

        System.setOut(out);
    }


}
