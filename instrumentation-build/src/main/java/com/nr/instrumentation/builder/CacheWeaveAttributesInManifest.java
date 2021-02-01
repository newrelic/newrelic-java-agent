/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.builder;

import com.newrelic.weave.weavepackage.WeavePackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Examine a single Jar file or a set of directories for cached weave attributes.
 *
 * The "single Jar" case is for an instrumentation module that might shadow in dependencies.
 * That shadow Jar must have all the classes in the correct packages.
 *
 * The "single directory" case is for a typical Java-only instrumentation module, where
 * there is no shading and the classes are in only one directory. Multiple directories
 * will be used for a Scala instrumentation module, as Scala classfiles end up
 * in a different directory.
 *
 * Scanning directories allows us to gather these attributes without building a jar
 * and then updating the attributes of the jar later.
 *
 * This class cannot exist in buildSrc while the newrelic-weaver is in this project.
 * buildSrc classes cannot have dependencies on projects yet to be built.
 *
 * This class is also invoked by a JVM that is forked from gradle. It therefore
 * does not have access to the gradle project or configuration.
 */
public class CacheWeaveAttributesInManifest {
    private static final WeavePackageFactory weavePackageFactory = new WeavePackageFactory();
    private static final ManifestAppender manifestAppender = new ManifestAppender();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            help();
            System.exit(1);
        }

        WeavePackage pkg = weavePackageFactory.createWeavePackage(Arrays.copyOfRange(args, 1, args.length));
        manifestAppender.copyAttributesToManifest(pkg);

        File outputDirectory = new File(args[0]).getParentFile();
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new Exception("Unable to create " + outputDirectory);
        }

        try (OutputStream stream = new FileOutputStream(args[0])) {
            manifestAppender.getManifest().write(stream);
        }
    }

    private static void help() {
        System.err.println("Print weave, illegal, or reference class names for a weave package jar.");
        System.err.println("The output attributes are intended for addition into the jar so we can quickly identify the contents of a jar.");
        System.err.println("Only execute this through the `jar` task modifications in gradle/script/instrumentation.gradle.");
    }

}