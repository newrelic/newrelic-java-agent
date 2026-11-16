/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext;
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
     * The fixed entry time Gradle and Shadow use for archives with preserveFileTimestamps = false (1980-02-01 00:00).
     * It is taken from Shadow so there is one definition of it. Used where the build writes zip entries itself
     * (the javadoc search index rewrite in the root build.gradle); the Shadow transformers call
     * {@link TransformerContext#getEntryTimestamp} directly.
     */
    public static final long CONSTANT_ENTRY_TIME = TransformerContext.getEntryTimestamp(false, 0L);

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
