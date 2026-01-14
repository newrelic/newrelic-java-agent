package com.newrelic.bootstrap;

import com.newrelic.test.marker.Flaky;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class EmbeddedJarFilesImplTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String originalTmpDir;

    @Before
    public void setUp() {
        originalTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempFolder.getRoot().getAbsolutePath());
    }

    @After
    public void tearDown() {
        if (originalTmpDir != null) {
            System.setProperty("java.io.tmpdir", originalTmpDir);
        }
        System.clearProperty("newrelic.config.temp_jarfile_age_threshold_hours");
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

    @Test
    public void cleanupStaleTempJarFiles_deletesOldJarFiles() throws Exception {
        File oldJar1 = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.jar", 10);
        File oldJar2 = createTempJarFile(BootstrapLoader.API_JAR_NAME + "67890.jar", 8);

        File newJar = createTempJarFile("agent-bridge99999.jar", 2);

        setThresholdAndCleanup(5);

        Assert.assertFalse("Old jar should be deleted", oldJar1.exists());
        Assert.assertFalse("Old jar should be deleted", oldJar2.exists());
        Assert.assertTrue("Recent jar should not be deleted", newJar.exists());
    }

    @Test
    public void cleanupStaleTempJarFiles_onlyDeletesMatchingJarNames() throws Exception {
        File matchingJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.jar", 10);
        File nonMatchingJar = createTempJarFile("whatever-lib12345.jar", 10);

        setThresholdAndCleanup(5);

        Assert.assertFalse("Matching jar should be deleted", matchingJar.exists());
        Assert.assertTrue("Non-matching jar should not be deleted", nonMatchingJar.exists());
    }

    @Test
    public void cleanupStaleTempJarFiles_onlyDeletesJarFiles() throws Exception {
        File oldJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.jar", 10);
        File oldTxt = createTempFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.txt", 10);

        setThresholdAndCleanup(5);

        Assert.assertFalse("Jar file should be deleted", oldJar.exists());
        Assert.assertTrue("Non-jar file should not be deleted", oldTxt.exists());
    }

    @Test
    @Category( Flaky.class )
    public void cleanupStaleTempJarFiles_respectsThresholdExactly() throws Exception {
        long now = System.currentTimeMillis();
        long fiveHoursInMillis = 5 * 60 * 60 * 1000L;

        // Create jar just before threshold - should be deleted
        File beforeThresholdJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "22222.jar", 0);
        beforeThresholdJar.setLastModified(now - fiveHoursInMillis - 1);

        // Create jar just after threshold - should not be deleted
        File afterThresholdJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "33333.jar", 0);
        afterThresholdJar.setLastModified(now - fiveHoursInMillis + 1);

        setThresholdAndCleanup(5);

        Assert.assertFalse("File older than threshold should be deleted", beforeThresholdJar.exists());
        Assert.assertTrue("File after threshold should not be deleted", afterThresholdJar.exists());
    }

    @Test
    public void cleanupStaleTempJarFiles_handlesMultipleMatchingPrefixes() throws Exception {
        File bridgeJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.jar", 10);
        File apiJar = createTempJarFile(BootstrapLoader.API_JAR_NAME + "67890.jar", 10);
        File weaverJar = createTempJarFile(BootstrapLoader.WEAVER_API_JAR_NAME + "11111.jar", 10);
        File securityJar = createTempJarFile(BootstrapLoader.NEWRELIC_SECURITY_AGENT + "22222.jar", 10);

        // Run cleanup with 5 hour threshold
        setThresholdAndCleanup(5);

        Assert.assertFalse("Bridge jar should be deleted", bridgeJar.exists());
        Assert.assertFalse("API jar should be deleted", apiJar.exists());
        Assert.assertFalse("Weaver jar should be deleted", weaverJar.exists());
        Assert.assertFalse("Security jar should be deleted", securityJar.exists());
    }

    @Test
    public void cleanupStaleTempJarFiles_zeroThreshold_doesNotDelete() throws Exception {
        File oldJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.jar", 10);

        setThresholdAndCleanup(0);

        Assert.assertTrue("File should not be deleted with 0 threshold", oldJar.exists());
    }

    @Test
    public void cleanupStaleTempJarFiles_negativeThreshold_doesNotDelete() throws Exception {
        File oldJar = createTempJarFile(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "12345.jar", 10);

        setThresholdAndCleanup(-5);

        Assert.assertTrue("File should not be deleted with negative threshold", oldJar.exists());
    }

    /**
     * Create a temporary jar file with a timestamp X hours in the past
     */
    private File createTempJarFile(String name, int hoursOld) throws Exception {
        return createTempFile(name, hoursOld);
    }

    /**
     * Create a temporary file with a timestamp X hours in the past
     */
    private File createTempFile(String name, int hoursOld) throws Exception {
        File file = new File(tempFolder.getRoot(), name);

        // Write some dummy content
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("dummy content".getBytes());
        }

        // Set last modified time to X hours in the past
        long hoursInMillis = hoursOld * 60L * 60L * 1000L;
        long pastTime = System.currentTimeMillis() - hoursInMillis;
        file.setLastModified(pastTime);

        return file;
    }

    /**
     * Set threshold and trigger cleanup by creating EmbeddedJarFilesImpl instance
     */
    private void setThresholdAndCleanup(int thresholdHours) {
        System.setProperty("newrelic.config.temp_jarfile_age_threshold_hours",
                String.valueOf(thresholdHours));

        // Constructor triggers cleanup
        EmbeddedJarFilesImpl.cleanupStaleTempJarFiles();
    }
}
