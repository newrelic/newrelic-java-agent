/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

import com.newrelic.weave.weavepackage.CachedWeavePackage;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class PrintAllManifestAttributes {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            help();
            System.exit(1);
        }

        WeavePackage pkg = createWeavePackage(args[0]);

        printColonSeparated(
                CachedWeavePackage.WEAVE_CLASSES_MANIFEST_ATTRIBUTE_NAME,
                pkg.getMatchTypes().keySet().stream().sorted().collect(Collectors.joining(","))
        );
        printColonSeparated(
                CachedWeavePackage.WEAVE_METHODS_MANIFEST_ATTRIBUTE_NAME,
                pkg.getMethodSignatures().stream().sorted().map(sig -> '"' + sig + '"').collect(Collectors.joining(","))
        );
        printColonSeparated(
                CachedWeavePackage.ILLEGAL_CLASSES_MANIFEST_ATTRIBUTE_NAME,
                pkg.getIllegalClasses().stream().sorted().collect(Collectors.joining(","))
        );
        printColonSeparated(
                CachedWeavePackage.REFERENCE_CLASSES_MANIFEST_ATTRIBUTE_NAME,
                pkg.getReferencedClassNames().stream().sorted().collect(Collectors.joining(","))
        );
        printColonSeparated(
                CachedWeavePackage.CLASS_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME,
                pkg.getAllRequiredAnnotationClasses().stream().sorted().collect(Collectors.joining(","))
        );
        printColonSeparated(
                CachedWeavePackage.METHOD_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME,
                pkg.getAllRequiredMethodAnnotationClasses().stream().sorted().collect(Collectors.joining(","))
        );
    }

    private static void printColonSeparated(String key, String value) {
        System.out.print(key);
        System.out.print(":");
        System.out.println(value);
    }

    private static void help() {
        System.err.println("Print weave, illegal, or reference class names for a weave package jar.");
        System.err.println("The output attributes are intended for addition into the jar so we can quickly identify the contents of a jar.");
        System.err.println("Only execute this through the `jar` task modifications in gradle/script/instrumentation.gradle.");
    }

    private static WeavePackage createWeavePackage(String jarLoc) throws Exception {
        JarInputStream jarInStream = null;
        try {
            URL weavePackageJarLoc = new File(jarLoc).toURI().toURL();
            WeavePackageConfig config = WeavePackageConfig.builder().url(weavePackageJarLoc).build();
            jarInStream = new JarInputStream(weavePackageJarLoc.openStream());
            return WeavePackage.createWeavePackage(jarInStream, config);
        } finally {
            if (null != jarInStream) {
                try {
                    jarInStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}