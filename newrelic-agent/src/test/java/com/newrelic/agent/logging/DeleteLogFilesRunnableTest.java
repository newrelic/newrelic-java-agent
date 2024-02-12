package com.newrelic.agent.logging;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class DeleteLogFilesRunnableTest {

    private DeleteLogFilesRunnable deleteLogFilesRunnable;

    @Test
    public void testDeleteOldFiles() throws Exception {

        long expectedLogFileCount;
        long actualLogFileCount;
        String fileNamePrefix = "testLogDir.";
        Path tempDir = Files.createTempDirectory(fileNamePrefix);
        addOldLogFiles(tempDir, 5, fileNamePrefix); // Add files created 5 days ago

        expectedLogFileCount = 5;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 3, fileNamePrefix); // Keep files for 3 days
        deleteLogFilesRunnable.run();

        expectedLogFileCount = 0;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        Files.deleteIfExists(tempDir);
    }

    @Test
    public void testSkipInvalidLogFiles() throws Exception {
        long expectedLogFileCount;
        long actualLogFileCount;
        String fileNamePrefix = "testLogDir.";
        Path tempDir = Files.createTempDirectory(fileNamePrefix);
        Files.createFile(tempDir.resolve("invalid_log_file.txt"));
        Files.createFile(tempDir.resolve("invalid.txt"));
        Files.createFile(tempDir.resolve("2024-02-04"));
        addOldLogFiles(tempDir, 10, fileNamePrefix);

        expectedLogFileCount = 8;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);

        deleteLogFilesRunnable = new DeleteLogFilesRunnable(tempDir, 1, fileNamePrefix);
        deleteLogFilesRunnable.run();

        expectedLogFileCount = 3;
        actualLogFileCount = Files.list(tempDir).count();

        assertEquals(expectedLogFileCount, actualLogFileCount);
        // Check if invalid log files are skipped
        assertTrue("Invalid log file should be skipped", Files.exists(tempDir.resolve("invalid_log_file.txt")));
        assertTrue("Invalid log file should be skipped", Files.exists(tempDir.resolve("invalid.txt")));
        assertTrue("Invalid log file should be skipped", Files.exists(tempDir.resolve("2024-02-04")));
        deleteTempDirAndContents(tempDir);
    }

    private void addOldLogFiles(Path directory, int daysAgo, String fileNamePrefix) throws IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 5; i++) {
            String fileName = fileNamePrefix + dateFormat.format(date) + "." + (i + 1);
            try {
                Files.createFile(directory.resolve(fileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            calendar.add(Calendar.MINUTE, 1);
            date = calendar.getTime();
        }
    }

    private void deleteTempDirAndContents(Path tempDir) throws IOException {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            Files.delete(tempDir);
    }
}
