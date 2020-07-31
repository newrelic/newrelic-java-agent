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
