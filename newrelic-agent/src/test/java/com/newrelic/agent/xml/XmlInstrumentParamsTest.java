package com.newrelic.agent.xml;

import java.io.File;

import com.newrelic.agent.xml.XmlInstrumentOptions;
import com.newrelic.agent.xml.XmlInstrumentParams;
import org.junit.Assert;
import org.junit.Test;

import static com.newrelic.agent.AgentHelper.getFile;

public class XmlInstrumentParamsTest {

    /** Test case one. */
    private static final String FILE_PATH_1 = "com/newrelic/agent/extension/util/test1.xml";
    /** Test file two. */
    private static final String FILE_PATH_2 = "com/newrelic/agent/extension/util/test2.xml";


    @Test
    public void testFile() {
        try {
            XmlInstrumentParams params = new XmlInstrumentParams();
            File theFile = getFile(FILE_PATH_1);
            String[] files = new String[] { theFile.getAbsolutePath() };
            params.setFile(files, XmlInstrumentOptions.FILE_PATH.getFlagName());

            Assert.assertEquals(theFile.getAbsolutePath(), params.getFile().getAbsolutePath());
            Assert.assertEquals(theFile.length(), params.getFile().length());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFileDoesNotExist() {
        try {
            XmlInstrumentParams params = new XmlInstrumentParams();
            String[] files = new String[] { "/tmp/ThisIsTest123456789.txt" };
            params.setFile(files, XmlInstrumentOptions.FILE_PATH.getFlagName());
            Assert.fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            // should go into here b/c the file does not exist.
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNoFiles() {
        try {
            XmlInstrumentParams params = new XmlInstrumentParams();
            String[] files = new String[] {};
            params.setFile(files, XmlInstrumentOptions.FILE_PATH.getFlagName());
            Assert.fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            // should go into here b/c the file does not exist.
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            XmlInstrumentParams params = new XmlInstrumentParams();
            String[] files = null;
            params.setFile(files, XmlInstrumentOptions.FILE_PATH.getFlagName());
            Assert.fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            // should go into here b/c the file does not exist.
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFileTooMany() {
        try {
            XmlInstrumentParams params = new XmlInstrumentParams();
            String[] files = new String[] { getFile(FILE_PATH_1).getAbsolutePath(),
                    getFile(FILE_PATH_2).getAbsolutePath() };
            params.setFile(files, XmlInstrumentOptions.FILE_PATH.getFlagName());
            Assert.fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            // should go into here b/c the file does not exist.
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
