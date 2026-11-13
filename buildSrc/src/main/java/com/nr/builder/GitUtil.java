package com.nr.builder;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GitUtil {
    // 9999-12-31T23:59:59Z. Rejects values that are clearly not seconds, such as milliseconds.
    private static final long MAX_EPOCH_SECONDS = 253402300799L;
    private static final long GIT_TIMEOUT_SECONDS = 30;

    public static String sha(final Project project) {
        try {
            return firstLineOfGitOutput(project, "rev-parse", "HEAD");
        } catch (Exception e) {
            project.getLogger().error("Failed to determine SHA; assuming a null value", e);
        }
        return null;
    }

    /**
     * The build date for the manifest, formatted like {@link Date#toString()} but always in UTC so the result
     * depends only on the source. Uses SOURCE_DATE_EPOCH if set (see https://reproducible-builds.org/specs/source-date-epoch/),
     * otherwise the HEAD commit time, otherwise the current time if git is unavailable.
     */
    public static String commitDate(final Project project) {
        String sourceDateEpoch = System.getenv("SOURCE_DATE_EPOCH");
        if (sourceDateEpoch != null && !sourceDateEpoch.trim().isEmpty()) {
            long epochSeconds;
            try {
                epochSeconds = Long.parseLong(sourceDateEpoch.trim());
            } catch (NumberFormatException e) {
                throw new GradleException("SOURCE_DATE_EPOCH must be an integer number of seconds since the Unix epoch but was '"
                        + sourceDateEpoch + "'. Fix or unset the variable.", e);
            }
            if (epochSeconds < 0 || epochSeconds > MAX_EPOCH_SECONDS) {
                throw new GradleException("SOURCE_DATE_EPOCH must be between 0 and " + MAX_EPOCH_SECONDS + " (the year 9999) but was '"
                        + sourceDateEpoch + "'. It is in seconds, not milliseconds. Fix or unset the variable.");
            }
            return format(new Date(epochSeconds * 1000L));
        }

        try {
            // --no-show-signature: with log.showSignature=true git prints signature text before the format output
            long epochSeconds = Long.parseLong(firstLineOfGitOutput(project, "log", "-1", "--no-show-signature", "--format=%ct"));
            return format(new Date(epochSeconds * 1000L));
        } catch (Exception e) {
            project.getLogger().error("Failed to determine commit date; using the current time, so this build will not be reproducible. "
                    + "Set SOURCE_DATE_EPOCH to build reproducibly without git.", e);
        }
        return format(new Date());
    }

    private static String firstLineOfGitOutput(Project project, String... gitArguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(gitArguments));
        String description = "git " + String.join(" ", gitArguments);
        Process gitProcess = new ProcessBuilder(command)
                .directory(project.getProjectDir())
                // git's own error text (for example "dubious ownership") goes to the build output, and an unread stderr pipe can't block it
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        // Read stdout to the end before waiting, so a full pipe can't block git.
        String firstLine = null;
        try (InputStreamReader reader = new InputStreamReader(gitProcess.getInputStream());
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (firstLine == null) {
                    firstLine = line.trim();
                }
            }
        }

        try {
            if (!gitProcess.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                gitProcess.destroyForcibly();
                throw new GradleException(description + " did not finish within " + GIT_TIMEOUT_SECONDS + " seconds");
            }
        } catch (InterruptedException e) {
            gitProcess.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new GradleException(description + " was interrupted", e);
        }
        if (gitProcess.exitValue() != 0) {
            throw new GradleException(description + " exited with status " + gitProcess.exitValue());
        }
        if (firstLine == null || firstLine.isEmpty()) {
            throw new GradleException(description + " printed no output");
        }
        return firstLine;
    }

    private static String format(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }
}
