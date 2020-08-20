/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.process.CommandLineArgumentProvider;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This argument provider lazily gathers the arguments necessary to CacheWeaveAttributesInManifest
 * for the instrumentation module in the provided project.
 *
 * It is lazy because this class will be instantiated before the project is fully evaluated. That is,
 * the scala plugin will add a classesDir to the "main" sourceSet, which doesn't happen until after
 * this class is instantiated.
 */
public class AttributeCommandLineArgumentProvider implements CommandLineArgumentProvider {
    private final Project project;
    private final String outputFile;

    public AttributeCommandLineArgumentProvider(Project project, String outputFile) {
        this.project = project;
        this.outputFile = outputFile;
    }

    @Override
    public Iterable<String> asArguments() {
        Set<File> files = getScannedLocations();
        return Stream.concat(Stream.of(outputFile), files.stream().map(File::getAbsolutePath)).collect(Collectors.toList());
    }

    private Set<File> getScannedLocations() {
        // Use case: the instrumentation module has a shadow jar. There should only be one shadow jar containing
        // all the classes and their shaded dependencies.
        FileCollection suppliedArtifacts = project.getConfigurations().getByName("instrumentationWithDependencies").getArtifacts().getFiles();
        if (!suppliedArtifacts.isEmpty()) {
            Set<File> files = suppliedArtifacts.getFiles();
            if (files.size() == 1) {
                return suppliedArtifacts.getFiles();
            }
            throw new GradleException("The instrumentationWithDependencies configuration must have only zero or one artifacts attached.");
        }

        // Use case: all other cases where there may be more than one classesDir for a module.
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        return sourceSets.getByName("main").getOutput().getClassesDirs().getFiles();
    }
}
