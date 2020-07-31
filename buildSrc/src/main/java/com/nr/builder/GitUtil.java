package com.nr.builder;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GitUtil {
    public static String sha(final Project project) {
        try {
            Process gitProcess = new ProcessBuilder("git", "rev-parse", "HEAD")
                    .directory(project.getProjectDir())
                    .start();

            if (gitProcess.waitFor() != 0) {
                throw new GradleException("git rev-parse HEAD exited with status " + gitProcess.exitValue());
            }

            try (InputStreamReader reader = new InputStreamReader(gitProcess.getInputStream());
                 BufferedReader bufferedReader = new BufferedReader(reader)) {
                return bufferedReader.readLine().trim();
            }
        } catch (Exception e) {
            project.getLogger().error("Failed to determine SHA; assuming a null value", e);
        }
        return null;
    }

}
