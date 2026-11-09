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

public class GitUtil {
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
            try {
                return format(new Date(Long.parseLong(sourceDateEpoch.trim()) * 1000L));
            } catch (NumberFormatException e) {
                throw new GradleException("SOURCE_DATE_EPOCH must be an integer number of seconds since the Unix epoch but was '"
                        + sourceDateEpoch + "'. Fix or unset the variable.", e);
            }
        }

        try {
            long epochSeconds = Long.parseLong(firstLineOfGitOutput(project, "log", "-1", "--format=%ct"));
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
        Process gitProcess = new ProcessBuilder(command)
                .directory(project.getProjectDir())
                .start();

        if (gitProcess.waitFor() != 0) {
            throw new GradleException("git " + String.join(" ", gitArguments) + " exited with status " + gitProcess.exitValue());
        }

        try (InputStreamReader reader = new InputStreamReader(gitProcess.getInputStream());
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            return bufferedReader.readLine().trim();
        }
    }

    private static String format(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }
}
