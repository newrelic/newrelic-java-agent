/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Finds and caches class information using a {@link ClassFinder}, to be used during a transformation. This class is
 * threadsafe.
 */
public class ClassCache implements ClassInformationFinder {

    private static final byte[] NO_CLASS_BYTES = new byte[] {};
    private static final ClassInformation NO_CLASS_INFORMATION = new ClassInformation();

    private final ClassFinder classFinder;
    private final ConcurrentMap<String, Boolean> classExistsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, byte[]> classBytesCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ClassInformation> classInformationCache = new ConcurrentHashMap<>();

    public ClassCache(ClassFinder classFinder) {
        this.classFinder = classFinder;
    }

    /**
     * Determine whether or not the specified class can be resolved using the cache's {@link ClassFinder}. Results of
     * this method are cached.
     * 
     * @param internalName internal class name
     * @return <code>true</code> if the resource can be resovled, <code>false</code> otherwise
     */
    public boolean hasClassResource(String internalName) {
        Boolean result = classExistsCache.get(internalName);
        if (result != null) {
            return result;
        }

        URL resource = classFinder.findResource(internalName);
        boolean hasResource = resource != null;
        classExistsCache.putIfAbsent(internalName, hasResource);
        return hasResource;
    }

    /**
     * Get the byte[] representing the specified class using the cache's {@link ClassFinder}. Results of this method are
     * cached.
     * 
     * @param internalName internal class name
     * @return class bytes, or <code>null</code> if the class could not be found
     * @throws IOException
     */
    public byte[] getClassResource(String internalName) throws IOException {
        byte[] result = classBytesCache.get(internalName); // FIXME this classBytesCache doesn't contain the Context class
        if (result != null) {
            return result == NO_CLASS_BYTES ? null : result;
        }

        URL resource = classFinder.findResource(internalName); // FIXME fails here?
        if (resource == null) {
            classBytesCache.putIfAbsent(internalName, NO_CLASS_BYTES);
            return null;
        }

        result = Streams.read(resource.openStream(), true);
        classBytesCache.putIfAbsent(internalName, result);
        return result;
    }

    /**
     * Find the {@link ClassInformation} for the specified internal class name. Results of this method are cached.
     * 
     * @param internalName internal class name
     * @return the {@link ClassInformation} for the specified internal class name
     * @throws IOException
     */
    public ClassInformation getClassInformation(String internalName) throws IOException {
        ClassInformation result = classInformationCache.get(internalName);
        if (result != null) {
            return result == NO_CLASS_INFORMATION ? null : result;
        }

        byte[] classBytes = getClassResource(internalName);
        if (classBytes == null) {
            classInformationCache.putIfAbsent(internalName, NO_CLASS_INFORMATION);
            return null;
        }

        result = ClassInformation.fromClassBytes(classBytes);
        classInformationCache.putIfAbsent(internalName, result);
        return result;
    }
}
