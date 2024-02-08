package com.newrelic.agent.logging;

import java.io.File;
import java.util.Calendar;

public class DeleteLogFileRunnable implements Runnable {

    private final File logFilePath;
    private final int fileCountDays;


    public DeleteLogFileRunnable (String logFilePath, int fileCountDays) {
        this.logFilePath = new File(logFilePath);
        this.fileCountDays = fileCountDays;
    }

    @Override
    public void run() {

        // Get current time
        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();

        // Calculate the threshold time
        calendar.add()
        // Delete files older than the threshold time
    }
}
