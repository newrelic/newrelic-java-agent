package com.newrelic.agent.logging;

import com.newrelic.api.agent.NewRelic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Runnable class to clean up expired log files based on a specified threshold.
 * This class iterates over agent log files identifying those older than the specified
 * threshold, and deletes them.
 */
public class ClearExpiredLogsRunnable implements Runnable {

    /**
     * Regular expression pattern for matching the date format in log file names.
     * The date format follows the pattern yyyy-MM-dd.
     */
    private static final String DATE_REGEX = "\\.(\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01]))(\\.\\d)?$";

    /**
     * The compiled pattern for matching the date format in log file names.
     */
    private final Pattern pattern;
    private final Path logDirectoryPath;
    private final int daysToKeepFiles;

    /**
     * Constructs a ClearExpiredLogFilesRunnable object.
     *
     * @param fileCount the number of days to keep log files before deleting them
     * @param logfile   the full path and filename of the agent logfile
     */
    public ClearExpiredLogsRunnable(int fileCount, String logfile) {
        File absoluteLogFilename = new File(logfile);
        this.daysToKeepFiles = fileCount;
        this.logDirectoryPath = absoluteLogFilename.getParent() == null ? Paths.get("./") : Paths.get(absoluteLogFilename.getParent());

        String fileNamePrefix = absoluteLogFilename.getName();
        pattern = Pattern.compile(fileNamePrefix.replace(".", "\\.")
                + DATE_REGEX);
    }

    @Override
    public void run() {
        Path logDirectory = Paths.get(logDirectoryPath.toString());
        LocalDate thresholdDate = LocalDate.now().minusDays(daysToKeepFiles);

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

    private void deleteIfOlderThanThreshold(Path filePath, String dateString, LocalDate thresholdDate) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fileDate;

        if (dateString != null) {
            try {
                fileDate = LocalDate.parse(dateString, dateFormat);
                if (fileDate.isBefore(thresholdDate)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                // Logging failure to parse log file date or error deleting expired log
                NewRelic.getAgent().getLogger().log(Level.FINEST, "Error deleting expired log", e);
            }
        }
    }

    private String extractDateString(String fileName) {
        // Extract the date string from the file name using the pattern
        Matcher matcher = pattern.matcher(fileName);
        return matcher.find() ? matcher.group(1) : null;
    }
}
