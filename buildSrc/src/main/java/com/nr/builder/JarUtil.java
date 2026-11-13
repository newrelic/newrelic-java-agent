/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

public class JarUtil {
    /**
     * The fixed entry time Gradle uses for archives with preserveFileTimestamps = false (1980-02-01 00:00 local time,
     * which ZipEntry stores as the DOS time 1980-02-01 00:00 in any time zone). Used by transformers that write their
     * own zip entries, which would otherwise get the time of the build.
     */
    public static final long CONSTANT_ENTRY_TIME = new java.util.GregorianCalendar(1980, java.util.Calendar.FEBRUARY, 1, 0, 0, 0).getTimeInMillis();

    public static File getNewRelicJar(Project newrelicAgentProject) {
        Optional<Task> jarTaskSet = newrelicAgentProject.getTasksByName("newrelicVersionedAgentJar", false).stream().findAny();
        if (!jarTaskSet.isPresent()) {
            throw new GradleException("Could not find task: newrelicVersionedAgentJar in project: " + newrelicAgentProject.getName());
        }

        Task jarTask = jarTaskSet.get();
        if (!(jarTask instanceof Jar)) {
            throw new GradleException("task newrelicVersionedAgentJar is not a Jar task.");
        }

        return ((Jar)jarTask).getArchiveFile().get().getAsFile();
    }
}
