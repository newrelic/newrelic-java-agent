package com.newrelic.agent.logging;

import com.newrelic.api.agent.NewRelic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Runnable class to clean up expired log files based on a specified threshold.
 */
public class DeleteLogFilesRunnable implements Runnable {

    private static Pattern pattern;
    private final Path logDirectoryPath;
    private final int daysToKeepFiles;
    private final String fileNamePrefix;

    /**
     * Constructs a DeleteLogFilesRunnable object.
     *
     * @param logDirectoryPath the directory path where log files are located
     * @param fileCount        the number of days to keep log files before deleting them
     * @param filePrefixPath   the file prefix used to filter log files
     */
    public DeleteLogFilesRunnable(Path logDirectoryPath, int fileCount, String filePrefixPath) {
        NewRelic.getAgent().getLogger().log(Level.INFO, "DELEON: the filePrefixPath: " + filePrefixPath);

        this.logDirectoryPath = logDirectoryPath;
        this.daysToKeepFiles = fileCount;
        this.fileNamePrefix = extractFileNamePrefix(filePrefixPath);

        // Define the pattern for matching log file names
        pattern = Pattern.compile(fileNamePrefix.replace(".", "\\.")
                + "\\.(\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01]))(\\.\\d)?$");
    }

    @Override
    public void run() {
        Thread.currentThread().setName("New Relic Expiring Log File Cleanup");
        Path logDirectory = Paths.get(logDirectoryPath.toString());
        Date thresholdDate = new Date();

        try (Stream<Path> paths = Files.list(logDirectory)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                String dateString = extractDateString(fileName);
                deleteIfOlderThanThreshold(path, dateString, thresholdDate);
            });
        } catch (IOException e) {
            // Logging failure to list log files
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Error listing log files in: " + logDirectoryPath, e);
        }
    }

    private void deleteIfOlderThanThreshold(Path filePath, String dateString, Date thresholdDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar threshold = Calendar.getInstance();
        Date fileDate;

        if (dateString != null) {
            try {
                fileDate = dateFormat.parse(dateString);
                threshold.setTime(fileDate);
                threshold.add(Calendar.DAY_OF_YEAR, daysToKeepFiles);

                if (threshold.getTime().before(thresholdDate)) {
                    Files.delete(filePath);
                }
            } catch (ParseException | IOException e) {
                // Logging failure to parse log file date or error deleting expired log
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Error deleting or parsing log", e);
            }
        }
    }

    private String extractDateString(String fileName) {
        // Extract the date string from the file name using the pattern
        Matcher matcher = pattern.matcher(fileName);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractFileNamePrefix(String fileName) {
        // Extract the file name prefix from the given file path
        String[] parts = fileName.split("/");
        return parts[parts.length - 1];
    }
}
