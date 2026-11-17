package com.nr.builder;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
     *
     * <p>KEEP IN SYNC with the "Set SOURCE_DATE_EPOCH to the HEAD commit time" step in
     * .github/actions/setup-environment/action.yml: CI sets SOURCE_DATE_EPOCH with the same git command used below
     * (committer time of HEAD, {@code git -c log.showSignature=false log -1 --format=%ct}), and that value takes priority
     * over this method's own lookup. If the two ever get the timestamp differently, CI and local builds of the same
     * commit will have different Built-Date values.
     */
    public static String commitDate(final Project project) {
        return commitDate(project, System.getenv("SOURCE_DATE_EPOCH"));
    }

    // The environment variable is a parameter so tests can set it.
    static String commitDate(final Project project, String sourceDateEpoch) {
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
            // -c log.showSignature=false: with that setting true, git prints signature text before the format output. Unlike
            // the --no-show-signature option (git 2.10 and later), a -c setting is accepted by every git version.
            long epochSeconds = Long.parseLong(firstLineOfGitOutput(project, "-c", "log.showSignature=false", "log", "-1", "--format=%ct"));
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
        return firstLineOfOutput(project.getProjectDir(), GIT_TIMEOUT_SECONDS, command);
    }

    // The command and timeout are parameters so tests can use stand-ins for git.
    static String firstLineOfOutput(File directory, long timeoutSeconds, List<String> command) throws Exception {
        String description = String.join(" ", command);

        // Output goes to temp files instead of pipes, so the command can never block on a full pipe and the wait below can
        // time out whether or not it is still holding its output open. (ProcessBuilder.inheritIO() would not show stderr:
        // under the Gradle daemon the inherited stderr is /dev/null.)
        File stdout = File.createTempFile("git-stdout", ".txt");
        File stderr = null;
        try {
            stderr = File.createTempFile("git-stderr", ".txt");
            Process process = new ProcessBuilder(command)
                    .directory(directory)
                    .redirectOutput(stdout)
                    .redirectError(stderr)
                    .start();
            process.getOutputStream().close();

            try {
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new GradleException(description + " did not finish within " + timeoutSeconds + " seconds");
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new GradleException(description + " was interrupted", e);
            }

            if (process.exitValue() != 0) {
                String errorText = firstLine(stderr);
                throw new GradleException(description + " exited with status " + process.exitValue()
                        + (errorText.isEmpty() ? "" : ": " + errorText));
            }
            String output = firstLine(stdout);
            if (output.isEmpty()) {
                throw new GradleException(description + " printed no output");
            }
            return output;
        } finally {
            stdout.delete();
            if (stderr != null) {
                stderr.delete();
            }
        }
    }

    /** The first line of the file, trimmed, or an empty string if the file is empty. */
    private static String firstLine(File file) throws IOException {
        String text = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
        int lineEnd = text.indexOf('\n');
        return (lineEnd < 0 ? text : text.substring(0, lineEnd)).trim();
    }

    private static String format(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }
}
