package com.newrelic.agent.logging;

import com.newrelic.api.agent.NewRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class DeleteLogFilesRunnable implements Runnable {

    private static final Logger logger = LogManager.getLogger(DeleteLogFilesRunnable.class);
    private static Pattern pattern;
    private final Path logDirectoryPath;
    private final int daysToKeepFiles;
    private final String fileNamePrefix;


    public DeleteLogFilesRunnable(Path logDirectoryPath, int fileCount, String fileNamePrefix) {
        this.logDirectoryPath = logDirectoryPath;
        this.daysToKeepFiles = fileCount;
        this.fileNamePrefix = fileNamePrefix;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("New Relic Expiring Log File Cleanup");
        Path logDirectory = Paths.get(logDirectoryPath.toString());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // TODO: create date instance here then pass

        try (Stream<Path> paths = Files.list(logDirectory)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (isValidLogFileName(fileName)) {
                    try {
                        deleteIfOlderThanThreshold(path, fileName, dateFormat);
                    } catch (IOException | ParseException e) {
                        if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                            logger.error("Error deleting log file: " + logDirectoryPath + fileName, e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                logger.error("Error listing log files in directory: " + logDirectoryPath, e);
            }
        }
    }

    private void deleteIfOlderThanThreshold(Path filePath, String fileName, SimpleDateFormat dateFormat) throws ParseException, IOException {

        String dateString = extractDateString(fileName);
        Date fileDate = dateFormat.parse(dateString);

        Calendar threshold = Calendar.getInstance();
        threshold.setTime(fileDate);
        threshold.add(Calendar.DAY_OF_YEAR, daysToKeepFiles);

        if (threshold.getTime().before(new Date())) {
            try {
                Files.delete(filePath);
                if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                    logger.error("Daily log file deleted: " + filePath); //TODO: Not necessary to log file deletion
                }
            } catch (IOException e) {
                if (NewRelic.getAgent().getLogger().isLoggable(Level.FINEST)) {
                    logger.error("Error deleting expired log file: " + filePath, e);
                }
            }
        }
    }

    private boolean isValidLogFileName(String fileName) {
        return fileName.startsWith(fileNamePrefix);
    }

    private String extractDateString(String fileName) {
    //TODO: unable to use matcher method outside of instrumentation package
        pattern = Pattern.compile(fileNamePrefix.replace(".", "\\.") + "\\.(\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01]))$");
//        if (matcher.find()) {
//            System.out.println(matcher.group(0));
//            return matcher.group(1);
//        }
        int startIndex = fileNamePrefix.length();
        int endIndex = Math.min(startIndex + "yyyy-MM-dd".length(), fileName.length());
        return fileName.substring(startIndex, endIndex);
    }
}
