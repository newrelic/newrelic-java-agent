package com.newrelic.bootstrap;

import com.newrelic.weave.utils.JarUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EmbeddedJarFilesImplTest {

    /**
     * This is run to create tmp jar files that match the jar file that ship with the agent. They're
     * cleaned up when the JVM exits
     */
    @Before
    public void setup() throws IOException {
        final String[] INTERNAL_JAR_FILE_NAMES = new String[] { BootstrapLoader.AGENT_BRIDGE_JAR_NAME,
                BootstrapLoader.API_JAR_NAME, BootstrapLoader.WEAVER_API_JAR_NAME,  BootstrapLoader.NEWRELIC_SECURITY_AGENT};

        Map<String, byte []> byteArrayMap = new HashMap<>();
        byteArrayMap.put("FakeClass1", "test".getBytes());
        byteArrayMap.put("FakeClass2", "test".getBytes());
        File tmpFile = JarUtils.createJarFile("prefix", byteArrayMap);
        Path tmpFilePath = Paths.get(tmpFile.getAbsolutePath());

        for (String filename : INTERNAL_JAR_FILE_NAMES) {
            Path newPath = Paths.get(filename + ".jar");
            Files.copy(tmpFilePath, newPath, StandardCopyOption.REPLACE_EXISTING).toFile().deleteOnExit();
        }
    }

    @Test
    public void noArgConstructor_initsWithInternalJarFileNames() {
        EmbeddedJarFiles embeddedJarFiles = new EmbeddedJarFilesImpl();

        Assert.assertTrue(Arrays.asList(embeddedJarFiles.getEmbeddedAgentJarFileNames()).contains(BootstrapLoader.AGENT_BRIDGE_JAR_NAME));
        Assert.assertTrue(Arrays.asList(embeddedJarFiles.getEmbeddedAgentJarFileNames()).contains(BootstrapLoader.API_JAR_NAME));
        Assert.assertTrue(Arrays.asList(embeddedJarFiles.getEmbeddedAgentJarFileNames()).contains(BootstrapLoader.WEAVER_API_JAR_NAME));
        Assert.assertTrue(Arrays.asList(embeddedJarFiles.getEmbeddedAgentJarFileNames()).contains(BootstrapLoader.NEWRELIC_SECURITY_AGENT));
    }

    @Test
    public void stringArrayConstructor_initsWithSuppliedJarNames() {
        String [] names = {"name1", "name2"};
        EmbeddedJarFiles embeddedJarFiles = new EmbeddedJarFilesImpl(names);

        Assert.assertTrue(Arrays.asList(embeddedJarFiles.getEmbeddedAgentJarFileNames()).contains("name1"));
        Assert.assertTrue(Arrays.asList(embeddedJarFiles.getEmbeddedAgentJarFileNames()).contains("name2"));
    }

    @Test
    public void getJarFileInAgent_with_ValidFilename_ReturnsTargetJarFile() throws IOException {
        EmbeddedJarFiles embeddedJarFiles = new EmbeddedJarFilesImpl();
        Assert.assertNotNull(embeddedJarFiles.getJarFileInAgent(BootstrapLoader.AGENT_BRIDGE_JAR_NAME));
    }

    @Test(expected = java.util.concurrent.CompletionException.class)
    public void getJarFileInAgent_withInvalidFilename_throwsException() throws IOException {
        EmbeddedJarFiles embeddedJarFiles = new EmbeddedJarFilesImpl();
        Assert.assertNotNull(embeddedJarFiles.getJarFileInAgent("foo"));
    }
}
