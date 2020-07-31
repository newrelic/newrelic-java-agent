/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.newrelic.weave.WeaveTestUtils;

/**
 * ClassCacheTest.java
 */
public class ClassCacheTest {
    private static final String CLASS_NAME_EXISTS = ClassCache.class.getName();
    private static final String CLASS_NAME_DOES_NOT_EXIST = "joe.loves.Pizza";

    private CountingClassFinder finder;
    private ClassCache cache;

    @Before
    public void before() {
        finder = new CountingClassFinder(new ClassLoaderFinder(Thread.currentThread().getContextClassLoader()));
        cache = new ClassCache(finder);
    }

    @Test
    public void testHasClassResourceExists() {
        boolean exists = cache.hasClassResource(CLASS_NAME_EXISTS);
        assertTrue(exists);
        assertEquals(1, finder.getCount(CLASS_NAME_EXISTS));

        exists = cache.hasClassResource(CLASS_NAME_EXISTS);
        assertTrue(exists);
        assertEquals(1, finder.getCount(CLASS_NAME_EXISTS));
    }

    @Test
    public void testHasClassResourceDoesNotExist() {
        boolean exists = cache.hasClassResource(CLASS_NAME_DOES_NOT_EXIST);
        assertFalse(exists);
        assertEquals(1, finder.getCount(CLASS_NAME_DOES_NOT_EXIST));

        exists = cache.hasClassResource(CLASS_NAME_DOES_NOT_EXIST);
        assertFalse(exists);
        assertEquals(1, finder.getCount(CLASS_NAME_DOES_NOT_EXIST));
    }

    @Test
    public void testGetClassResourceExists() throws IOException {
        byte[] expected = WeaveTestUtils.getClassBytes(CLASS_NAME_EXISTS);
        byte[] actual = cache.getClassResource(CLASS_NAME_EXISTS);
        assertArrayEquals(expected, actual);
        assertEquals(1, finder.getCount(CLASS_NAME_EXISTS));

        actual = cache.getClassResource(CLASS_NAME_EXISTS);
        assertArrayEquals(expected, actual);
        assertEquals(1, finder.getCount(CLASS_NAME_EXISTS));
    }

    @Test
    public void testGetClassResourceDoesNotExist() throws IOException {
        byte[] classBytes = cache.getClassResource(CLASS_NAME_DOES_NOT_EXIST);
        assertNull(classBytes);
        assertEquals(1, finder.getCount(CLASS_NAME_DOES_NOT_EXIST));

        classBytes = cache.getClassResource(CLASS_NAME_DOES_NOT_EXIST);
        assertNull(classBytes);
        assertEquals(1, finder.getCount(CLASS_NAME_DOES_NOT_EXIST));
    }

    @Test
    public void testGetClassInformationExists() throws IOException {
        ClassInformation expected = ClassInformation.fromClassBytes(WeaveTestUtils.getClassBytes(CLASS_NAME_EXISTS));
        ClassInformation actual = cache.getClassInformation(CLASS_NAME_EXISTS);
        assertEquals(expected.className, CLASS_NAME_EXISTS.replace('.', '/'));
        assertEquals(expected, actual);
        assertEquals(1, finder.getCount(CLASS_NAME_EXISTS));

        actual = cache.getClassInformation(CLASS_NAME_EXISTS);
        assertEquals(expected, actual);
        assertEquals(1, finder.getCount(CLASS_NAME_EXISTS));
    }

    @Test
    public void testGetClassInformationDoesNotExist() throws IOException {
        ClassInformation classInformation = cache.getClassInformation(CLASS_NAME_DOES_NOT_EXIST);
        assertNull(classInformation);
        assertEquals(1, finder.getCount(CLASS_NAME_DOES_NOT_EXIST));

        classInformation = cache.getClassInformation(CLASS_NAME_DOES_NOT_EXIST);
        assertNull(classInformation);
        assertEquals(1, finder.getCount(CLASS_NAME_DOES_NOT_EXIST));
    }

    private static class CountingClassFinder implements ClassFinder {
        private final Multiset<String> counts = HashMultiset.create();
        private final ClassFinder delegate;

        public CountingClassFinder(ClassFinder delegate) {
            this.delegate = delegate;
        }

        @Override
        public URL findResource(String internalName) {
            counts.add(internalName);
            return delegate.findResource(internalName);
        }

        public int getCount(String internalName) {
            return counts.count(internalName);
        }
    }
}
