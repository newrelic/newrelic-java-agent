package com.newrelic.bootstrap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class EmbeddedJarFilesImplTest {

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
    @Ignore
    public void getJarFileInAgent_with_ValidFilename_ReturnsTargetJarFile() throws IOException {
        EmbeddedJarFiles embeddedJarFiles = new EmbeddedJarFilesImpl();
        Assert.assertNotNull(embeddedJarFiles.getJarFileInAgent(BootstrapLoader.API_JAR_NAME));
    }

    @Test(expected = java.util.concurrent.CompletionException.class)
    public void getJarFileInAgent_withInvalidFilename_throwsException() throws IOException {
        EmbeddedJarFiles embeddedJarFiles = new EmbeddedJarFilesImpl();
        Assert.assertNotNull(embeddedJarFiles.getJarFileInAgent("foo"));
    }
}
