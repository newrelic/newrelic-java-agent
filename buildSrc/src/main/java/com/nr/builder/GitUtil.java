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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GitUtil {
    // 9999-12-31T23:59:59Z. Rejects values that are clearly not seconds, such as milliseconds.
    private static final long MAX_EPOCH_SECONDS = 253402300799L;
    private static final long GIT_TIMEOUT_SECONDS = 30;

    /**
     * The variables git itself calls repository-local (the list printed by {@code git rev-parse --local-env-vars}, which
     * differs between git versions, so it is written out here). If one is set, git works on that repository or index
     * instead of the one in the project directory. A Gradle daemon keeps the environment of the build that started it, so
     * a build run from a git hook, or one that left {@code GIT_DIR=} behind, would otherwise make this class stamp the
     * wrong commit or fail to read any.
     */
    static final List<String> REPOSITORY_ENVIRONMENT_VARIABLES = Collections.unmodifiableList(Arrays.asList(
            "GIT_ALTERNATE_OBJECT_DIRECTORIES", "GIT_COMMON_DIR", "GIT_CONFIG", "GIT_CONFIG_COUNT", "GIT_CONFIG_PARAMETERS",
            "GIT_DIR", "GIT_GRAFT_FILE", "GIT_IMPLICIT_WORK_TREE", "GIT_INDEX_FILE", "GIT_INTERNAL_SUPER_PREFIX", "GIT_NO_REPLACE_OBJECTS",
            "GIT_OBJECT_DIRECTORY", "GIT_PREFIX", "GIT_REPLACE_REF_BASE", "GIT_SHALLOW_FILE", "GIT_WORK_TREE"));

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
        return firstLineOfOutput(project.getProjectDir(), null, GIT_TIMEOUT_SECONDS, command);
    }

    static String firstLineOfOutput(File directory, File tempDirectory, long timeoutSeconds, List<String> command) throws Exception {
        return firstLineOfOutput(directory, tempDirectory, timeoutSeconds, command, Collections.<String, String>emptyMap());
    }

    // The command, timeout and temp directory are parameters so tests can use stand-ins for git and see what is left behind.
    // A null tempDirectory means the default temp directory. additionalEnvironment is added to the inherited environment
    // before the repository variables are removed, so tests can simulate a daemon that inherited them.
    static String firstLineOfOutput(File directory, File tempDirectory, long timeoutSeconds, List<String> command,
            Map<String, String> additionalEnvironment) throws Exception {
        String description = String.join(" ", command);

        // Output goes to temp files instead of pipes, so the command can never block on a full pipe and the wait below can
        // time out whether or not it is still holding its output open. (ProcessBuilder.inheritIO() would not show stderr:
        // under the Gradle daemon the inherited stderr is /dev/null.)
        File stdout = File.createTempFile("git-stdout", ".txt", tempDirectory);
        File stderr = null;
        try {
            stderr = File.createTempFile("git-stderr", ".txt", tempDirectory);
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(directory)
                    .redirectOutput(stdout)
                    .redirectError(stderr);
            processBuilder.environment().putAll(additionalEnvironment);
            processBuilder.environment().keySet().removeAll(REPOSITORY_ENVIRONMENT_VARIABLES);
            Process process = processBuilder.start();
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
