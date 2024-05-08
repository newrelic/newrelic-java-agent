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

import static org.junit.Assert.*;

public class ClearExpiredLogsRunnableTest {

    private ClearExpiredLogsRunnable clearExpiredLogsRunnable;
    private Path tempDir;
    private String absoluteLogFilePath;
    private final static String LOG_FILENAME = "newrelic.log";

    @Before
    public void setUp() throws IOException {
        String fileNamePrefix = "nr_log_test";
        tempDir = Files.createTempDirectory(fileNamePrefix);
        absoluteLogFilePath = tempDir.toString() + "/" + LOG_FILENAME;
        assertNotNull(tempDir);
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
                            System.out.println("IOException when attempting to delete file: " + path);
                        }
                    });
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    public void testRunnableAgainstEmptyLogDirectory() throws IOException {
        clearExpiredLogsRunnable = new ClearExpiredLogsRunnable(3, absoluteLogFilePath);
        clearExpiredLogsRunnable.run();

        long actualLogFileCount = Files.list(tempDir).count();
        assertEquals(0, actualLogFileCount);
    }

    @Test
    public void testDeleteOldFilesWithLogExtensions() throws Exception {
        addOldLogFilesWithExtensions(10);

        long expectedLogFileCount = 5;
        long actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        clearExpiredLogsRunnable = new ClearExpiredLogsRunnable(3, absoluteLogFilePath);
        clearExpiredLogsRunnable.run();

        expectedLogFileCount = 0;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);
    }

    @Test
    public void testDeleteOldFilesWithoutLogExtensions() throws IOException {
        addOldLogFilesWithoutExtensions(10);

        long expectedLogFileCount = 5;
        long actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        clearExpiredLogsRunnable = new ClearExpiredLogsRunnable(3, absoluteLogFilePath);
        clearExpiredLogsRunnable.run();

        expectedLogFileCount = 0;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);
    }

    @Test
    public void testSkipInvalidLogFiles() throws Exception {
        Files.createFile(tempDir.resolve("invalid_log_file.txt"));
        Files.createFile(tempDir.resolve("invalid.txt"));
        Files.createFile(tempDir.resolve("2024-02-04"));
        addOldLogFilesWithExtensions(10);

        long expectedLogFileCount = 8;
        long actualLogFileCount = Files.list(tempDir).count();
        assertEquals(expectedLogFileCount, actualLogFileCount);

        clearExpiredLogsRunnable = new ClearExpiredLogsRunnable(1, absoluteLogFilePath);
        clearExpiredLogsRunnable.run();

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

        clearExpiredLogsRunnable = new ClearExpiredLogsRunnable(3, absoluteLogFilePath);
        clearExpiredLogsRunnable.run();

        assertTrue("Read-only file should still exist", Files.exists(readOnlyFile));
    }

    @Test
    public void testDirectoryWithMixedFiles() throws IOException {
        addOldLogFilesWithExtensions(5);
        addOldLogFilesWithoutExtensions(5);

        // non-log files
        Files.createFile(tempDir.resolve("testFile1.txt"));
        Files.createFile(tempDir.resolve("testFile2.jpg"));
        Files.createDirectory(tempDir.resolve("subdirectory"));
        Files.createFile(tempDir.resolve("subdirectory").resolve("testFile3.pdf"));

        long expectedLogFileCount = 13;
        long actualLogFileCount = Files.list(tempDir).count();
        assertEquals(expectedLogFileCount, actualLogFileCount);

        clearExpiredLogsRunnable = new ClearExpiredLogsRunnable(3, absoluteLogFilePath);
        clearExpiredLogsRunnable.run();

        assertTrue("Non-log file should exist", Files.exists(tempDir.resolve("testFile1.txt")));
        assertTrue("Non-log file should exist", Files.exists(tempDir.resolve("testFile2.jpg")));
        assertTrue("Non-log file should exist", Files.exists(tempDir.resolve("subdirectory").resolve("testFile3.pdf")));
        expectedLogFileCount = 6;
        actualLogFileCount = Files.list(tempDir).count();
        assertEquals(expectedLogFileCount, actualLogFileCount);
    }

    private void addOldLogFilesWithExtensions(int daysAgo) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 5; i++) {
            String fileName = LOG_FILENAME + "." + dateFormat.format(date) + "." + (i + 1);
            Files.createFile(tempDir.resolve(fileName));
            calendar.add(Calendar.MINUTE, 1);
            date = calendar.getTime();
        }
    }

    private void addOldLogFilesWithoutExtensions(int daysAgo) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 5; i++) {
            String fileName = LOG_FILENAME + "." + dateFormat.format(date);
            Files.createFile(tempDir.resolve(fileName));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            date = calendar.getTime();
        }
    }
}