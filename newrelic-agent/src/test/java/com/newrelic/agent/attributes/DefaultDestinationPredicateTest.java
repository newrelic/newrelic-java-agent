/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class DefaultDestinationPredicateTest {
    private static final String DEST = "test";

    @Test
    public void testAllEmpty() {
        Set<String> exclude = new HashSet<>();
        Set<String> include = new HashSet<>();
        Set<String> defaultExclude = new HashSet<>();
        Set<String> mandatoryExclude = new HashSet<>();

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertTrue(pred.apply("request"));
        Assert.assertTrue(pred.apply("message.parameters.foo"));
        Assert.assertTrue(pred.apply("jvm.thread_id"));
        Assert.assertTrue(pred.apply("nospecified"));
    }

    @Test
    public void testMandatoryWinsOverInclude() {
        Set<String> exclude = new HashSet<>();
        Set<String> include = Sets.newHashSet("request", "message.parameters.*", "jvm.thread_id");
        Set<String> defaultExclude = new HashSet<>();
        Set<String> mandatoryExclude = Sets.newHashSet("request", "message.parameters.*", "jvm*");

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertFalse(pred.apply("request"));
        Assert.assertFalse(pred.apply("message.parameters.foo"));
        Assert.assertFalse(pred.apply("jvm.thread_id"));
        Assert.assertTrue(pred.apply("nospecified"));
    }

    @Test
    public void testIncludeWinsOverDefaultExclude() {
        Set<String> exclude = new HashSet<>();
        Set<String> include = Sets.newHashSet("request", "message.parameters.*", "jvm.thread_id");
        Set<String> defaultExclude = Sets.newHashSet("request", "message.parameters.*", "jvm*");
        Set<String> mandatoryExclude = new HashSet<>();

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertTrue(pred.apply("request"));
        Assert.assertTrue(pred.apply("message.parameters.foo"));
        Assert.assertTrue(pred.apply("jvm.thread_id"));
        Assert.assertTrue(pred.apply("nospecified"));
    }

    @Test
    public void testExcludeWinsOverInclude() {
        Set<String> exclude = Sets.newHashSet("request", "message.parameters.*", "jvm*");
        Set<String> include = Sets.newHashSet("request", "message.parameters.*", "jvm.thread_id");
        Set<String> defaultExclude = new HashSet<>();
        Set<String> mandatoryExclude = new HashSet<>();

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertFalse(pred.apply("request"));
        Assert.assertFalse(pred.apply("message.parameters.foo"));
        // more specific wins over less
        Assert.assertTrue(pred.apply("jvm.thread_id"));
        Assert.assertTrue(pred.apply("nospecified"));
    }

    @Test
    public void testMandatoryWinsOverAll() {
        Set<String> exclude = Sets.newHashSet("request", "message.parameters.*", "jvm*");
        Set<String> include = Sets.newHashSet("request", "message.parameters.*", "jvm.thread_id");
        Set<String> defaultExclude = Sets.newHashSet("request", "message.parameters.*", "jvm*");
        Set<String> mandatoryExclude = Sets.newHashSet("request", "message.parameters.*", "jvm*");

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertFalse(pred.apply("request"));
        Assert.assertFalse(pred.apply("message.parameters.foo"));
        Assert.assertFalse(pred.apply("jvm.thread_id"));
        Assert.assertTrue(pred.apply("nospecified"));
    }

    @Test
    public void isPotentialConfigMatchNoIncludeExcludes() {
        Set<String> exclude = new HashSet<>();
        Set<String> include = new HashSet<>();
        Set<String> defaultExclude = Sets.newHashSet("request*");
        Set<String> mandatoryExclude = new HashSet<>();

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertFalse(pred.isPotentialConfigMatch("request.parameters.*"));

        defaultExclude.clear();
        mandatoryExclude = Sets.newHashSet("request*");

        pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude, mandatoryExclude);
        Assert.assertFalse(pred.isPotentialConfigMatch("request.parameters.*"));
    }

    @Test
    public void isPotentialConfigMatchExcludes() {
        Set<String> exclude = Sets.newHashSet("request.parameters.*");
        Set<String> include = new HashSet<>();
        Set<String> defaultExclude = new HashSet<>();
        Set<String> mandatoryExclude = new HashSet<>();

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertFalse(pred.isPotentialConfigMatch("request.parameters.*"));
    }

    @Test
    public void isPotentialConfigMatchIncludes() {
        Set<String> exclude = new HashSet<>();
        Set<String> include = Sets.newHashSet("request.parameters.*", "bar", "tada", "hello", "random", "bear", "foo");
        Set<String> defaultExclude = new HashSet<>();
        Set<String> mandatoryExclude = new HashSet<>();

        DefaultDestinationPredicate pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude,
                mandatoryExclude);
        Assert.assertTrue(pred.isPotentialConfigMatch("request.parameters."));

        include = Sets.newHashSet("r*", "bar", "tada", "hello", "random", "bear", "foo");
        pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude, mandatoryExclude);
        Assert.assertTrue(pred.isPotentialConfigMatch("request.parameters."));

        include = Sets.newHashSet("request.parameters.foo", "bar", "tada", "hello", "random", "bear", "foo");
        pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude, mandatoryExclude);
        Assert.assertTrue(pred.isPotentialConfigMatch("request.parameters."));

        include = Sets.newHashSet("recent.parameters", "bar", "tada", "hello", "random", "bear", "foo");
        pred = new DefaultDestinationPredicate(DEST, exclude, include, defaultExclude, mandatoryExclude);
        Assert.assertFalse(pred.isPotentialConfigMatch("request.parameters."));
    }
}
