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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, EPOCH_2023));
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    @Test
    public void commitDate_rejectsValuesThatAreNotSecondsSinceTheEpoch() {
        for (String bad : new String[] {"abc", "1700000000.5", "-1", "1700000000000", "9223372036854775807"}) {
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
        commitWithCommitterDate(repository, EPOCH_2023, true);
        git(repository, "config", "log.showSignature", "true");

        // Proves the setup reproduces the problem: without the fix the first line is not the time.
        assertFalse(firstLineOfGitLog(repository, "log", "-1", "--format=%ct").equals(EPOCH_2023));

        assertEquals(DATE_2023, GitUtil.commitDate(repositoryProject, null));
    }

    @Test
    public void commitDate_fallsBackToTheCurrentTimeWhenGitCannotReadTheRepository() {
        long before = System.currentTimeMillis();
        String date = GitUtil.commitDate(plainDirectoryProject, null);
        assertNotNull(date);
        assertTrue(date, date.endsWith(" UTC " + java.time.Year.now(java.time.ZoneOffset.UTC)));
        assertTrue(System.currentTimeMillis() >= before);
    }

    @Test
    public void sha_returnsTheHeadCommit() {
        String sha = GitUtil.sha(repositoryProject);
        assertNotNull(sha);
        assertTrue(sha, sha.matches("[0-9a-f]{40}"));
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
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return false;
            }
        } catch (java.io.IOException e) {
            return false;
        }
        git(repository, "config", "gpg.format", "ssh");
        git(repository, "config", "user.signingkey", new File(repository, "signing-key.pub").getPath());
        return true;
    }

    private static String firstLineOfGitLog(File repository, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        File output = File.createTempFile("git-log", ".txt");
        try {
            Process process = new ProcessBuilder(command).directory(repository).redirectErrorStream(true)
                    .redirectOutput(output).start();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS));
            List<String> lines = java.nio.file.Files.readAllLines(output.toPath());
            return lines.isEmpty() ? "" : lines.get(0).trim();
        } finally {
            output.delete();
        }
    }

    private static void git(File directory, String... arguments) throws Exception {
        gitWithEnvironment(directory, null, null, arguments);
    }

    private static void gitWithEnvironment(File directory, String variable, String value, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory).redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.remove("GIT_DIR");
        environment.remove("GIT_WORK_TREE");
        if (variable != null) {
            environment.put(variable, value);
        }
        File output = File.createTempFile("git-test", ".txt");
        try {
            Process process = builder.redirectOutput(output).start();
            if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                fail("git " + String.join(" ", arguments) + " failed");
            }
        } finally {
            output.delete();
        }
    }
}
