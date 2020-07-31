/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pointcuts and JMX use some type annotations. This class uses org.reflections
 * to build a list of classes that are annotated with
 * these specific annotations.
 */
public class BuildManifestTask extends DefaultTask {
    private static final List<String> SCANNED_ANNOTATION_TYPES = Arrays.asList(
            "com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper",
            "com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin",
            "com.newrelic.agent.instrumentation.pointcuts.PointCut",
            "com.newrelic.agent.jmx.metrics.JmxInit"
    );

    @TaskAction
    public void main() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(mapRuntimeClasspathToURLs())
                .setExpandSuperTypes(false)
                .setScanners(new TypeAnnotationsScanner().filterResultsBy(SCANNED_ANNOTATION_TYPES::contains))
                .setSerializer(new PropertySerializer()));
        reflections.save(getOutputFilePath());
    }

    @OutputFile
    public String getOutputFilePath() {
        return getOutputDirectory().toPath().resolve("PointcutClasses.properties").toString();
    }

    private File outputDirectory = getProject().getBuildDir().toPath().resolve("pointcutClasses").toFile();
    private FileCollection inputRuntimeClasspath;

    private File getOutputDirectory() {
        return outputDirectory;
    }

    private Collection<URL> mapRuntimeClasspathToURLs() {
        return getInputRuntimeClasspath().getFiles().stream()
                .map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @InputFiles
    public FileCollection getInputRuntimeClasspath() {
        return inputRuntimeClasspath;
    }

    public void setInputRuntimeClasspath(FileCollection inputRuntimeClasspath) {
        this.inputRuntimeClasspath = inputRuntimeClasspath;
    }
}
