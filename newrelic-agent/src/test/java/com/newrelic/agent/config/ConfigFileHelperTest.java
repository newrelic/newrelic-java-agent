package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigFileHelperTest {
    File tmpConfigFile;
    String parentDir;

    @Before
    public void setup() throws IOException {
        parentDir = Files.createTempDirectory("nr").toFile().getAbsolutePath();
        tmpConfigFile = new File(parentDir, "newrelic.yml");
        tmpConfigFile.createNewFile();
        tmpConfigFile.deleteOnExit();
    }

    @Test
    public void findConfigFile_fromProperty_returnsFileInstance() throws IOException {
        String oldValue = System.setProperty("newrelic.config.file", tmpConfigFile.getCanonicalPath());
        File f = ConfigFileHelper.findConfigFile();

        Assert.assertNotNull(f);
        Assert.assertEquals(tmpConfigFile.getName(), f.getName());

        System.setProperty("newrelic.config.file", oldValue == null ? "" : oldValue);
    }

    @Test
    public void findConfigFile_fromNewRelicDirectory_returnsFileInstance() {
        String oldConfigValue = System.clearProperty("newrelic.config.file");
        String oldHomeValue = System.setProperty("newrelic.home", tmpConfigFile.getParent());
        File f = ConfigFileHelper.findConfigFile();

        Assert.assertNotNull(f);
        Assert.assertEquals(tmpConfigFile.getName(), f.getName());

        System.setProperty("newrelic.home", oldHomeValue == null ? "" : oldHomeValue);
        System.setProperty("newrelic.config.file", oldConfigValue == null ? "" : oldConfigValue);
    }

    @Test
    public void findConfigFile_fromCurrentWorkingDirectory_returnsFileInstance() throws IOException {
        String oldConfigValue = System.clearProperty("newrelic.config.file");
        String oldHomeValue = System.clearProperty("newrelic.home");

        // Create the yml file in the current working folder
        File currentFolderFile = new File( "newrelic.yml");
        currentFolderFile.createNewFile();
        currentFolderFile.deleteOnExit();

        File f = ConfigFileHelper.findConfigFile();

        Assert.assertNotNull(f);
        Assert.assertEquals(currentFolderFile.getName(), f.getName());

        System.setProperty("newrelic.home", oldHomeValue == null ? "" : oldHomeValue);
        System.setProperty("newrelic.config.file", oldConfigValue == null ? "" : oldConfigValue);
    }
}
