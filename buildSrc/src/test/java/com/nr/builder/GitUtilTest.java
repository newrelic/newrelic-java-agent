/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GitUtilTest {
    // 2023-11-14T22:13:20Z
    private static final String EPOCH_2023 = "1700000000";
    private static final String DATE_2023 = "Tue Nov 14 22:13:20 UTC 2023";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Project repositoryProject;
    private Project plainDirectoryProject;

    @Before
    public void setUp() throws Exception {
        File repository = temporaryFolder.newFolder("repository");
        git(repository, "init", "-q", ".");
        git(repository, "-c", "user.name=test", "-c", "user.email=test@example.com", "-c", "commit.gpgsign=false",
                "commit", "-q", "--allow-empty", "-m", "initial");
        repositoryProject = ProjectBuilder.builder().withProjectDir(repository).build();
        plainDirectoryProject = ProjectBuilder.builder().withProjectDir(temporaryFolder.newFolder("plain")).build();
    }

    @Test
    public void commitDate_usesSourceDateEpoch() {
        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, EPOCH_2023));
        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, "  " + EPOCH_2023 + "\n"));
    }

    @Test
    public void commitDate_isInUtcWhateverTheDefaultTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, EPOCH_2023));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void commitDate_rejectsValuesThatAreNotSecondsSinceTheEpoch() {
        for (String bad : new String[] { "abc", "1700000000.5", "-1", "1700000000000", "9223372036854775807", "253402300800" }) {
            try {
                GitUtil.commitDate(repositoryProject, bad);
                fail("Expected a GradleException for SOURCE_DATE_EPOCH=" + bad);
            } catch (GradleException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("SOURCE_DATE_EPOCH"));
                assertTrue(e.getMessage(), e.getMessage().contains(bad));
            }
        }
    }

    @Test
    public void commitDate_acceptsTheFirstAndLastSecondOfTheAllowedRange() {
        assertEquals("Thu Jan 01 00:00:00 UTC 1970", GitUtil.commitDate(repositoryProject, "0"));
        assertEquals("Fri Dec 31 23:59:59 UTC 9999", GitUtil.commitDate(repositoryProject, "253402300799"));
    }

    @Test
    public void commitDate_readsTheSourceDateEpochEnvironmentVariable() {
        // buildSrc/build.gradle sets the variable for the test JVM. When the tests run some other way (an IDE), skip.
        Assume.assumeTrue(EPOCH_2023.equals(System.getenv("SOURCE_DATE_EPOCH")));
        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject));
    }

    @Test
    public void commitDate_usesTheHeadCommitTimeWhenSourceDateEpochIsUnsetOrBlank() throws Exception {
        commitWithCommitterDate(repositoryProject.getProjectDir(), EPOCH_2023, false);

        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, null));
        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, ""));
        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, "   "));
    }

    /**
     * With log.showSignature=true, git log prints signature text before the format output for a signed commit, which
     * would make the first line of output something other than the commit time.
     */
    @Test
    public void commitDate_ignoresLogShowSignatureForASignedHeadCommit() throws Exception {
        File repository = repositoryProject.getProjectDir();
        Assume.assumeTrue("ssh-keygen is needed to sign a commit", sshKeygenWorks(repository));
        try {
            commitWithCommitterDate(repository, EPOCH_2023, true);
        } catch (AssertionError e) {
            // git older than 2.34 has no gpg.format=ssh, and ssh-keygen older than 8.1 cannot sign.
            Assume.assumeNoException("this git or ssh-keygen cannot make SSH-signed commits", e);
        }
        git(repository, "config", "log.showSignature", "true");

        // Proves the setup reproduces the problem: without the fix the first line is not the time.
        assertFalse(GitUtil.firstLineOfOutput(repository, null, 30, Arrays.asList("git", "log", "-1", "--format=%ct")).equals(EPOCH_2023));

        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, null));
    }

    @Test
    public void commitDate_fallsBackToTheCurrentTimeWhenGitCannotReadTheRepository() throws Exception {
        long before = System.currentTimeMillis();
        String date = GitUtil.commitDate(plainDirectoryProject, null);
        long after = System.currentTimeMillis();

        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        long reported = format.parse(date).getTime();
        assertTrue(date, date.contains(" UTC "));
        // The date has one second resolution.
        assertTrue(date, reported >= before - 1000 && reported <= after);
    }

    @Test
    public void firstLineOfOutput_returnsTheFirstLineTrimmed() throws Exception {
        assumeUnixShell();
        assertEquals("first", GitUtil.firstLineOfOutput(temporaryFolder.getRoot(), null, 30, shell("printf '  first  \\nsecond\\n'")));
    }

    @Test
    public void firstLineOfOutput_failsWithTheErrorTextWhenTheCommandFails() throws Exception {
        assumeUnixShell();
        try {
            GitUtil.firstLineOfOutput(temporaryFolder.getRoot(), null, 30, shell("echo 'fatal: not a repo' >&2; exit 3"));
            fail("Expected a GradleException");
        } catch (GradleException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("exited with status 3: fatal: not a repo"));
        }
    }

    @Test
    public void firstLineOfOutput_failsWhenTheCommandPrintsNothing() throws Exception {
        assumeUnixShell();
        try {
            GitUtil.firstLineOfOutput(temporaryFolder.getRoot(), null, 30, shell("exit 0"));
            fail("Expected a GradleException");
        } catch (GradleException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("printed no output"));
        }
    }

    @Test
    public void firstLineOfOutput_failsAndKillsTheCommandWhenItDoesNotFinishInTime() throws Exception {
        assumeUnixShell();
        File pidFile = new File(temporaryFolder.getRoot(), "pid");
        long start = System.nanoTime();
        try {
            // exec, so the recorded process id is the one that has to be killed.
            GitUtil.firstLineOfOutput(temporaryFolder.getRoot(), null, 1, shell("echo $$ > '" + pidFile.getPath() + "'; exec sleep 60"));
            fail("Expected a GradleException");
        } catch (GradleException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("did not finish within 1 seconds"));
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        // Rules out a timeout in the wrong unit (1 millisecond) and a wait for the whole 60 seconds.
        assertTrue("waited " + elapsedMillis + " ms", elapsedMillis >= 900 && elapsedMillis < 30000);

        String pid = new String(Files.readAllBytes(pidFile.toPath()), StandardCharsets.UTF_8).trim();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (isRunning(pid) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertFalse("the command (process " + pid + ") was left running", isRunning(pid));
    }

    private static boolean isRunning(String pid) throws Exception {
        return new ProcessBuilder("kill", "-0", pid).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")))
                .start().waitFor() == 0;
    }

    @Test
    public void firstLineOfOutput_failsWhenTheCommandCannotBeStarted() throws Exception {
        try {
            GitUtil.firstLineOfOutput(temporaryFolder.getRoot(), null, 30, Collections.singletonList("no-such-command-for-gitutil-test"));
            fail("Expected an IOException");
        } catch (IOException expected) {
            // The caller (commitDate or sha) logs this and falls back.
        }
    }

    @Test
    public void firstLineOfOutput_removesItsTemporaryFilesOnEveryPath() throws Exception {
        assumeUnixShell();
        // A private temp directory, so files from other tests or other processes cannot be mistaken for leaks.
        File tempDirectory = temporaryFolder.newFolder("own-temp");
        File workingDirectory = temporaryFolder.getRoot();

        GitUtil.firstLineOfOutput(workingDirectory, tempDirectory, 30, shell("echo ok"));
        assertNoFilesIn(tempDirectory, "success");

        for (String failingScript : new String[] { "exit 1", "exit 0", "echo oops >&2; exit 2" }) {
            try {
                GitUtil.firstLineOfOutput(workingDirectory, tempDirectory, 30, shell(failingScript));
                fail("Expected a GradleException for: " + failingScript);
            } catch (GradleException expected) {
                // expected
            }
            assertNoFilesIn(tempDirectory, failingScript);
        }

        try {
            GitUtil.firstLineOfOutput(workingDirectory, tempDirectory, 1, shell("sleep 60"));
            fail("Expected a timeout");
        } catch (GradleException expected) {
            // expected
        }
        assertNoFilesIn(tempDirectory, "timeout");

        try {
            GitUtil.firstLineOfOutput(workingDirectory, tempDirectory, 30, Collections.singletonList("no-such-command-for-gitutil-test"));
            fail("Expected an IOException");
        } catch (IOException expected) {
            // expected
        }
        assertNoFilesIn(tempDirectory, "cannot start");
    }

    private static void assertNoFilesIn(File directory, String situation) {
        String[] leftOver = directory.list();
        assertTrue("files left behind after " + situation + ": " + Arrays.toString(leftOver), leftOver == null || leftOver.length == 0);
    }

    private static List<String> shell(String script) {
        return Arrays.asList("sh", "-c", script);
    }

    private static void assumeUnixShell() {
        Assume.assumeTrue("needs a Unix sh", new File("/bin/sh").canExecute());
    }

    @Test
    public void sha_ignoresRepositoryVariablesInheritedFromTheEnvironment() throws Exception {
        String expected = GitUtil.firstLineOfOutput(repositoryProject.getProjectDir(), null, 30, Arrays.asList("git", "rev-parse", "HEAD"));
        Map<String, String> polluted = new HashMap<>();
        polluted.put("GIT_DIR", "");
        polluted.put("GIT_WORK_TREE", "/nonexistent/work-tree");
        polluted.put("GIT_INDEX_FILE", "/nonexistent/index");
        polluted.put("GIT_COMMON_DIR", "/nonexistent/common");

        assertEquals(expected, GitUtil.firstLineOfOutput(repositoryProject.getProjectDir(), null, 30,
                Arrays.asList("git", "rev-parse", "HEAD"), polluted));
    }

    @Test
    public void firstLineOfOutput_removesOnlyTheRepositoryVariables() throws Exception {
        assumeUnixShell();
        Map<String, String> environment = new HashMap<>();
        for (String name : GitUtil.REPOSITORY_ENVIRONMENT_VARIABLES) {
            environment.put(name, "set");
        }
        for (String name : GitUtil.KEPT_LOCAL_ENVIRONMENT_VARIABLES) {
            environment.put(name, "kept");
        }
        environment.put("GIT_CONFIG_GLOBAL", "kept");
        environment.put("UNRELATED_VARIABLE_FOR_GITUTIL_TEST", "kept");

        String script = "echo \"$(env | grep -c -E '^(" + String.join("|", GitUtil.REPOSITORY_ENVIRONMENT_VARIABLES)
                + ")=') $GIT_CONFIG_GLOBAL $GIT_CONFIG_COUNT $GIT_CONFIG_PARAMETERS $UNRELATED_VARIABLE_FOR_GITUTIL_TEST\"";
        assertEquals("0 kept kept kept kept", GitUtil.firstLineOfOutput(temporaryFolder.getRoot(), null, 30, shell(script), environment));
    }

    @Test
    public void repositoryEnvironmentVariables_includeEveryVariableTheInstalledGitCallsLocal() throws Exception {
        File output = temporaryFolder.newFile("local-env-vars.txt");
        Process process = new ProcessBuilder("git", "rev-parse", "--local-env-vars")
                .redirectError(ProcessBuilder.Redirect.to(temporaryFolder.newFile("local-env-vars-stderr.txt"))).redirectOutput(output).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        Assume.assumeTrue("this git does not know --local-env-vars", process.exitValue() == 0);

        for (String name : Files.readAllLines(output.toPath())) {
            // Only variable names count: trace or advice text on stdout must not fail the build.
            if (name.trim().matches("GIT_[A-Z0-9_]+")) {
                assertTrue(name + " is local to a repository for this git, so add it to GitUtil.REPOSITORY_ENVIRONMENT_VARIABLES "
                                + "(or to KEPT_LOCAL_ENVIRONMENT_VARIABLES if it only carries configuration)",
                        GitUtil.REPOSITORY_ENVIRONMENT_VARIABLES.contains(name.trim())
                                || GitUtil.KEPT_LOCAL_ENVIRONMENT_VARIABLES.contains(name.trim()));
            }
        }
    }

    @Test
    public void sha_returnsTheHeadCommit() throws Exception {
        String sha = GitUtil.sha(repositoryProject);
        assertNotNull(sha);
        assertTrue(sha, sha.matches("[0-9a-f]{40}"));
        // The test JVM runs inside the agent's own work tree, so the commit of the project directory has to be the one read.
        assertEquals(GitUtil.firstLineOfOutput(repositoryProject.getProjectDir(), null, 30, Arrays.asList("git", "rev-parse", "HEAD")), sha);
        assertNotEquals(GitUtil.firstLineOfOutput(new File("."), null, 30, Arrays.asList("git", "rev-parse", "HEAD")), sha);
    }

    @Test
    public void sha_isNullWhenGitCannotReadTheRepository() {
        assertNull(GitUtil.sha(plainDirectoryProject));
    }

    private static void commitWithCommitterDate(File repository, String epochSeconds, boolean signed) throws Exception {
        List<String> arguments = new ArrayList<>(Arrays.asList("-c", "user.name=test", "-c", "user.email=test@example.com",
                "-c", "commit.gpgsign=" + signed));
        arguments.addAll(Arrays.asList("commit", "-q", "--amend", "--allow-empty", "-m", "dated"));
        gitWithEnvironment(repository, "GIT_COMMITTER_DATE", epochSeconds + " +0000", arguments.toArray(new String[0]));
    }

    private static boolean sshKeygenWorks(File repository) throws Exception {
        try {
            Process process = new ProcessBuilder("ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-f", new File(repository, "signing-key").getPath())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.to(new File(repository.getParentFile(), "ssh-keygen.log"))).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            if (process.exitValue() != 0) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        git(repository, "config", "gpg.format", "ssh");
        git(repository, "config", "user.signingkey", new File(repository, "signing-key.pub").getPath());
        return true;
    }

    private static void git(File directory, String... arguments) throws Exception {
        gitWithEnvironment(directory, null, null, arguments);
    }

    private static void gitWithEnvironment(File directory, String variable, String value, String... arguments) throws Exception {
        // No hooks: a developer's global commit hooks must not decide whether the test repository can be set up.
        String[] command = new String[arguments.length + 3];
        command[0] = "git";
        command[1] = "-c";
        command[2] = "core.hooksPath=/dev/null";
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory).redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.keySet().removeAll(GitUtil.REPOSITORY_ENVIRONMENT_VARIABLES);
        if (variable != null) {
            environment.put(variable, value);
        }
        File output = File.createTempFile("git-test", ".txt");
        try {
            Process process = builder.redirectOutput(output).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("git " + String.join(" ", arguments) + " did not finish within 30 seconds");
            }
            if (process.exitValue() != 0) {
                fail("git " + String.join(" ", arguments) + " exited with status " + process.exitValue() + ": "
                        + new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8).trim());
            }
        } finally {
            output.delete();
        }
    }
}
