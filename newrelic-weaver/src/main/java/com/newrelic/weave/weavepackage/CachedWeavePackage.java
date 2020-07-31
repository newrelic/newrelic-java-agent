/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.Streams;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Rather than process the entire weave jar, this weave package stores a list of reference and illegal class names. When
 * a validation is requested, the reference and illegal names are used to fail the validation fast. If it is not
 * possible to fail fast, the weave package jar is processed.
 * <p/>
 * Since most internal WeavePackages will not load, this allows us to fail invalid packages faster and avoid unnecessary
 * work around jar processing for failed packages.
 * <p/>
 * This class exists for performance reasons. To see the functionality of the weaver look at {@link WeavePackage}
 */
public class CachedWeavePackage extends WeavePackage {
    public static final String REFERENCE_CLASSES_MANIFEST_ATTRIBUTE_NAME = "Reference-Classes";
    public static final String ILLEGAL_CLASSES_MANIFEST_ATTRIBUTE_NAME = "Illegal-Classes";
    public static final String WEAVE_CLASSES_MANIFEST_ATTRIBUTE_NAME = "Weave-Classes";
    public static final String WEAVE_METHODS_MANIFEST_ATTRIBUTE_NAME = "Weave-Methods";
    public static final String CLASS_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME = "Class-Required-Annotations";
    public static final String METHOD_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME = "Method-Required-Annotations";

    private final Set<String> referenceClasses;
    private final Set<String> weaveClasses;
    private final Set<String> weaveMethods;
    private final Set<String> illegalClasses;
    private final Set<String> classRequiredAnnotations;
    private final Set<String> methodRequiredAnnotations;
    private final URL jarLocation;
    private volatile boolean loaded = false;

    private static final Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();

    public static WeavePackage createWeavePackage(URL jarLocation, JarInputStream jarStream, WeavePackageConfig config)
            throws Exception {
        Attributes attributes = jarStream.getManifest().getMainAttributes();
        String weaveMethods = attributes.getValue(WEAVE_METHODS_MANIFEST_ATTRIBUTE_NAME);
        String weaveClasses = attributes.getValue(WEAVE_CLASSES_MANIFEST_ATTRIBUTE_NAME);
        String classRequiredAnnotations = attributes.getValue(CLASS_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME);
        String methodRequiredAnnotations = attributes.getValue(METHOD_REQUIRED_ANNOTATIONS_MANIFEST_ATTRIBUTE_NAME);
        if (weaveMethods == null || weaveClasses == null || classRequiredAnnotations == null || methodRequiredAnnotations == null) {
            return WeavePackage.createWeavePackage(jarStream, config);
        } else {
            String referenceClasses = attributes.getValue(REFERENCE_CLASSES_MANIFEST_ATTRIBUTE_NAME);
            String illegalClasses = attributes.getValue(ILLEGAL_CLASSES_MANIFEST_ATTRIBUTE_NAME);

            Set<String> weaveMethodsList = new HashSet<>();
            for (String weaveMethod : splitter.split(weaveMethods)) {
                weaveMethodsList.add(weaveMethod.replaceAll("\"", ""));
            }

            Set<String> weaveClassesList = Sets.newHashSet(splitter.split(weaveClasses));
            Set<String> referenceList = Sets.newHashSet(splitter.split(referenceClasses));
            Set<String> classAnnotationsList = Sets.newHashSet(splitter.split(classRequiredAnnotations));
            Set<String> methodAnnotationsList = Sets.newHashSet(splitter.split(methodRequiredAnnotations));
            Set<String> illegalList = illegalClasses == null ? null : Sets.newHashSet(splitter.split(illegalClasses));

            return new CachedWeavePackage(jarLocation, config, weaveMethodsList, weaveClassesList, referenceList,
                    illegalList, classAnnotationsList, methodAnnotationsList);
        }
    }

    CachedWeavePackage(URL jarLocation, WeavePackageConfig config, Set<String> weaveMethods, Set<String> weaveClasses,
            Set<String> referenceClasses, Set<String> illegalClasses, Set<String> classRequiredAnnotations,
            Set<String> methodRequiredAnnotations) {
        super(config, new ArrayList<byte[]>());
        this.weaveMethods = weaveMethods;
        this.weaveClasses = weaveClasses;
        this.referenceClasses = referenceClasses;
        this.illegalClasses = illegalClasses;
        this.classRequiredAnnotations = classRequiredAnnotations;
        this.methodRequiredAnnotations = methodRequiredAnnotations;
        this.jarLocation = jarLocation;
        if (isBootstrapClassName(this.weaveClasses)) {
            this.setWeavesBootstrap();
        }
    }

    @Override
    public boolean hasMatcher(String className, String[] superNames, String[] interfaceNames,
            Set<String> classAnnotations, Set<String> methodAnnotations, ClassCache classCache) throws IOException {
        Set<String> matchClassNames = Sets.newHashSet(className);
        if (superNames != null && superNames.length > 0) {
            Collections.addAll(matchClassNames, superNames);
        }
        if (interfaceNames != null && interfaceNames.length > 0) {
            Collections.addAll(matchClassNames, interfaceNames);
        }

        boolean isClassAnnotationMatch = classRequiredAnnotations != null && !classRequiredAnnotations.isEmpty();
        boolean isMethodAnnotationMatch = methodRequiredAnnotations != null && !methodRequiredAnnotations.isEmpty();
        matchClassNames.retainAll(weaveClasses);
        if (!isClassAnnotationMatch && !isMethodAnnotationMatch && matchClassNames.isEmpty()) {
            return false;
        }

        this.load();
        return super.hasMatcher(className, superNames, interfaceNames, classAnnotations, methodAnnotations, classCache);
    }

    @Override
    public PackageValidationResult validate(ClassCache cache) throws IOException {
        if (loaded) {
            return super.validate(cache);
        } else {
            PackageValidationResult fastResult = new PackageValidationResult(this, cache, referenceClasses,
                    illegalClasses);
            if (fastResult.succeeded()) {
                load();
                return super.validate(cache);
            } else {
                return fastResult;
            }
        }
    }

    @Override
    public Set<String> getRequiredClasses() {
        Set<String> requiredClasses = new HashSet<>();
        requiredClasses.addAll(weaveClasses);
        requiredClasses.addAll(referenceClasses);
        return requiredClasses;
    }

    @Override
    public Set<String> getMethodSignatures() {
        return new HashSet<>(weaveMethods);
    }


    @Override
    public Set<String> getAllRequiredAnnotationClasses() {
        return new HashSet<>(classRequiredAnnotations);
    }

    @Override
    public Set<String> getAllRequiredMethodAnnotationClasses() {
        return new HashSet<>(methodRequiredAnnotations);
    }

    /**
     * Fully load the WeavePackage from the JAR file.
     */
    private void load() throws IOException {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    JarInputStream jarStream = null;
                    try {
                        List<byte[]> classBytes = new ArrayList<>();
                        jarStream = new JarInputStream(jarLocation.openStream());
                        JarEntry entry = null;
                        while ((entry = jarStream.getNextJarEntry()) != null) {
                            if (entry.getName().endsWith(".class")) {
                                classBytes.add(Streams.read(jarStream, false));
                            }
                        }
                        super.processWeaveBytes(classBytes);
                        this.loaded = true;
                    } finally {
                        if (jarStream != null) {
                            jarStream.close();
                        }
                    }
                }
            }
        }
    }

}
