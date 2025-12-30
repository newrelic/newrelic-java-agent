package com.newrelic.bootstrap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public class EmbeddedJarFilesImplTest {

    private static final long TWO_SEC = 2000L;

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
    public void cleanupCalledOnStartup_deletesOldFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("nr-agent-test-temp");
        try {
            // Configure agent to use our temp dir and make cleanup aggressive for test
            System.setProperty("newrelic.tempdir", tempDir.toString());
            System.clearProperty("newrelic.tempdir.cleanup.disable");
            System.setProperty("newrelic.tempdir.cleanup.age.ms", "100"); // 100ms

            // Create fake old agent jar files that match the INTERNAL_JAR_FILE_NAMES pattern
            File file1 = tempDir.resolve(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "-old.jar").toFile();
            File file2 = tempDir.resolve(BootstrapLoader.API_JAR_NAME + "-old.jar").toFile();

            Assert.assertTrue("Failed to create test file1", file1.createNewFile());
            Assert.assertTrue("Failed to create test file2", file2.createNewFile());

            // Set last modified to be older than the cutoff (now - 1000ms)
            long oldTime = System.currentTimeMillis() - 1000L;
            Assert.assertTrue(file1.setLastModified(oldTime));
            Assert.assertTrue(file2.setLastModified(oldTime));

            // Sanity check files exist before cleanup
            Assert.assertTrue(file1.exists());
            Assert.assertTrue(file2.exists());

            // Ensure cleanup is enabled and CRaC simulation is disabled for this test run
            System.setProperty("newrelic.tempdir.cleanup.disable", "false");
            System.clearProperty("newrelic.tempdir.cleanup.crac.present");
            // Force cleanup in tests to avoid accidental CRaC detection interfering
            System.setProperty("newrelic.tempdir.cleanup.force", "true");

            File configuredTemp = BootstrapLoader.getTempDir();
            Assert.assertNotNull("BootstrapLoader.getTempDir() should return our test temp dir", configuredTemp);
            Assert.assertEquals("Temp dir mismatch", tempDir.toFile().getAbsolutePath(), configuredTemp.getAbsolutePath());
            Assert.assertEquals("cleanup.disable must be false", "false", System.getProperty("newrelic.tempdir.cleanup.disable"));
            Assert.assertNull("crac.present should be unset for this test", System.getProperty("newrelic.tempdir.cleanup.crac.present"));

             // Invoke cleanup directly to avoid timing/classloading issues with constructor-run cleanup
             EmbeddedJarFilesImpl ej = new EmbeddedJarFilesImpl();
            try {
                java.lang.reflect.Method m = EmbeddedJarFilesImpl.class.getDeclaredMethod("cleanupOldAgentTempFiles");
                m.setAccessible(true);
                m.invoke(ej);
            } catch (ReflectiveOperationException roe) {
                throw new RuntimeException(roe);
            }

            // The cleanup runs synchronously, but file deletion may be slightly delayed on some platforms.
            // Wait up to 2 seconds for the files to be removed to avoid flaky failures.
            long waitUntil = System.currentTimeMillis() + TWO_SEC;
            while (System.currentTimeMillis() < waitUntil && (file1.exists() || file2.exists())) {
                Thread.sleep(50);
            }

            Assert.assertFalse("Old file1 should have been deleted by cleanup", file1.exists());
            Assert.assertFalse("Old file2 should have been deleted by cleanup", file2.exists());
        } finally {
            // Clear system properties and remove temp dir
            System.clearProperty("newrelic.tempdir");
            System.clearProperty("newrelic.tempdir.cleanup.age.ms");
            System.clearProperty("newrelic.tempdir.cleanup.disable");
            System.clearProperty("newrelic.tempdir.cleanup.force");

            try {
                try (java.util.stream.Stream<Path> s = Files.walk(tempDir)) {
                    s.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(f -> {
                                try {
                                    if (!f.delete()) {
                                        System.err.println("Failed to delete test file: " + f.getAbsolutePath());
                                    }
                                } catch (Throwable ignored) {
                                }
                            });
                }
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void cleanupSkippedWhenCRacPresent_doesNotDeleteOldFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("nr-agent-test-temp-crac");
        try {
            // Configure agent to use our temp dir and make cleanup aggressive for test
            System.setProperty("newrelic.tempdir", tempDir.toString());
            System.clearProperty("newrelic.tempdir.cleanup.disable");
            System.setProperty("newrelic.tempdir.cleanup.age.ms", "100"); // 100ms

            // Create fake old agent jar files that match the INTERNAL_JAR_FILE_NAMES pattern
            File file1 = tempDir.resolve(BootstrapLoader.AGENT_BRIDGE_JAR_NAME + "-old.jar").toFile();
            File file2 = tempDir.resolve(BootstrapLoader.API_JAR_NAME + "-old.jar").toFile();

            Assert.assertTrue("Failed to create test file1", file1.createNewFile());
            Assert.assertTrue("Failed to create test file2", file2.createNewFile());

            // Set last modified to be older than the cutoff (now - 1000ms)
            long oldTime = System.currentTimeMillis() - 1000L;
            Assert.assertTrue(file1.setLastModified(oldTime));
            Assert.assertTrue(file2.setLastModified(oldTime));

            // Sanity check files exist before cleanup
            Assert.assertTrue(file1.exists());
            Assert.assertTrue(file2.exists());

            // The presence of the test-only jdk.crac.Checkpoint class on the test classpath
            // should cause EmbeddedJarFilesImpl to detect CRaC and skip cleanup.
            System.setProperty("newrelic.tempdir.cleanup.crac.present", "true");
            try {
                new EmbeddedJarFilesImpl();
            } finally {
                System.clearProperty("newrelic.tempdir.cleanup.crac.present");
            }

            // Because CRaC marker class is present, cleanup should have been skipped and files remain
            Assert.assertTrue("Old file1 should NOT have been deleted when CRaC API present", file1.exists());
            Assert.assertTrue("Old file2 should NOT have been deleted when CRaC API present", file2.exists());
        } finally {
            // Clear system properties and remove temp dir
            System.clearProperty("newrelic.tempdir");
            System.clearProperty("newrelic.tempdir.cleanup.age.ms");
            System.clearProperty("newrelic.tempdir.cleanup.disable");

            try {
                try (java.util.stream.Stream<Path> s = Files.walk(tempDir)) {
                    s.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(f -> {
                                try {
                                    if (!f.delete()) {
                                        System.err.println("Failed to delete test file: " + f.getAbsolutePath());
                                    }
                                } catch (Throwable ignored) {
                                }
                            });
                }
            } catch (IOException ignored) {
            }
        }
    }
}
