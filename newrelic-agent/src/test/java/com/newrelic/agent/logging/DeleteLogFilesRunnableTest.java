package com.newrelic.agent.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeleteLogFilesRunnableTest {

    private DeleteLogFilesRunnable deleteLogFilesRunnable;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        String fileNamePrefix = "testLogDir";
        tempDir = Files.createTempDirectory(fileNamePrefix);
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    public void testRunnableAgainstEmptyLogDirectory() throws IOException {
        tempDir = Files.createTempDirectory("emptyLogDir");

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 3, "testLogDir");
        deleteLogFilesRunnable.run();

        long actualLogFileCount = Files.list(tempDir).count();
        assertEquals(0, actualLogFileCount);
    }

    @Test
    public void testDeleteOldFilesWithLogExtensions() throws Exception {
        addOldLogFilesWithExtensions(10, "testLogDir");

        long expectedLogFileCount = 5;
        long actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 3, "testLogDir");
        deleteLogFilesRunnable.run();

        expectedLogFileCount = 0;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);
    }

    @Test
    public void testDeleteOldFilesWithoutLogExtensions() throws IOException {
        addOldLogFilesWithoutExtensions(10, "testLogDir");

        long expectedLogFileCount = 5;
        long actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 3, "testLogDir");
        deleteLogFilesRunnable.run();

        expectedLogFileCount = 0;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);
    }

    @Test
    public void testSkipInvalidLogFiles() throws Exception {
        Files.createFile(tempDir.resolve("invalid_log_file.txt"));
        Files.createFile(tempDir.resolve("invalid.txt"));
        Files.createFile(tempDir.resolve("2024-02-04"));
        addOldLogFilesWithExtensions(10, "testLogDir");

        long expectedLogFileCount = 8;
        long actualLogFileCount = Files.list(tempDir).count();
        assertEquals(expectedLogFileCount, actualLogFileCount);

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 1, "testLogDir");
        deleteLogFilesRunnable.run();

        expectedLogFileCount = 3;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);
        assertTrue("Invalid log file should be skipped", Files.exists(tempDir.resolve("invalid_log_file.txt")));
        assertTrue("Invalid log file should be skipped", Files.exists(tempDir.resolve("invalid.txt")));
        assertTrue("Invalid log file should be skipped", Files.exists(tempDir.resolve("2024-02-04")));
    }

    @Test
    public void testNoPermissionToDeleteFiles() throws IOException {
        Path readOnlyFile = Files.createFile(tempDir.resolve("readonly.log"));
        readOnlyFile.toFile().setReadOnly();

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 3, "testLogDir");
        deleteLogFilesRunnable.run();

        assertTrue("Read-only file should still exist", Files.exists(readOnlyFile));
    }

    private void addOldLogFilesWithExtensions(int daysAgo, String fileNamePrefix) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 5; i++) {
            String fileName = fileNamePrefix + "." + dateFormat.format(date) + "." + (i + 1);
            Files.createFile(tempDir.resolve(fileName));
            calendar.add(Calendar.MINUTE, 1);
            date = calendar.getTime();
        }
    }

    private void addOldLogFilesWithoutExtensions(int daysAgo, String fileNamePrefix) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 5; i++) {
            String fileName = fileNamePrefix + "." + dateFormat.format(date);
            Files.createFile(tempDir.resolve(fileName));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            date = calendar.getTime();
        }
    }
}
