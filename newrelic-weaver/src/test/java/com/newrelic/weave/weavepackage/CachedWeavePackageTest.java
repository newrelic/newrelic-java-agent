/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * CachedWeavePackageTest.java
 */
public class CachedWeavePackageTest {

    @Test
    public void testHasMatcher() throws IOException {
        CachedWeavePackage weavePackage = new CachedWeavePackage(new URL("http://does.not.exist"),
                WeavePackageConfig.builder().name("name").source("source").build(), ImmutableSet.of("somemethod"),
                ImmutableSet.of("someclassname"), null, null, null, null);
        assertFalse(weavePackage.hasMatcher("one", new String[] { "two" }, new String[] { "three" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null));

        try {
            weavePackage.hasMatcher("someclassname", new String[] { "two" }, new String[] { "three" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null);
            fail();
        } catch (IOException ioe) {
        }

        try {
            weavePackage.hasMatcher("one", new String[] { "someclassname" }, new String[] { "three" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null);
            fail();
        } catch (IOException ioe) {
        }

        try {
            weavePackage.hasMatcher("one", new String[] { "two" }, new String[] { "someclassname" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null);
            fail();
        } catch (IOException ioe) {
        }
    }

    @Test
    public void testNoMatchReference() throws IOException {
        // Don't match on reference classes, only weave classes.
        CachedWeavePackage weavePackage = new CachedWeavePackage(new URL("http://does.not.exist"),
                WeavePackageConfig.builder().name("name").source("source").build(), Collections.<String> emptySet(),
                ImmutableSet.of("somemethod"), ImmutableSet.of("someclassname"), null, null, null);
        assertFalse(weavePackage.hasMatcher("one", new String[] { "two" }, new String[] { "three" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null ));
        assertFalse(weavePackage.hasMatcher("someclassname", new String[] { "two" }, new String[] { "three" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null ));
        assertFalse(weavePackage.hasMatcher("one", new String[] { "someclassname" }, new String[] { "three" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null ));
        assertFalse(weavePackage.hasMatcher("one", new String[] { "two" }, new String[] { "someclassname" }, Collections.<String>emptySet(), Collections.<String>emptySet(), null ));
    }

    @Test
    public void testWeavesBootstrap() throws MalformedURLException {
        CachedWeavePackage weavePackage = new CachedWeavePackage(new URL("http://www.newrelic.com"),
                WeavePackageConfig.builder().name("name").source("source").build(), ImmutableSet.of("somemethod"),
                ImmutableSet.of("com.example.myclass"), ImmutableSet.of("java/lang/String"), Collections.<String> emptySet(),
                Collections.<String> emptySet(), null);
        assertFalse(weavePackage.weavesBootstrap());
    }

    @Test
    public void testNoWeavesBootstrap() throws MalformedURLException {
        CachedWeavePackage weavePackage = new CachedWeavePackage(new URL("http://www.newrelic.com"),
                WeavePackageConfig.builder().name("name").source("source").build(), ImmutableSet.of("somemethod"),
                ImmutableSet.of("java.lang.String"), ImmutableSet.of("something/or/other"), Collections.<String> emptySet(),
                Collections.<String> emptySet(), null);
        assertTrue(weavePackage.weavesBootstrap());
    }
}
