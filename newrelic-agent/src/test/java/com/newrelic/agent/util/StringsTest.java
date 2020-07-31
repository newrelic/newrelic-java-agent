/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class StringsTest {
    @Test
    public void unquote() {
        Assert.assertEquals("dude", Strings.unquote("dude"));
        Assert.assertEquals("dude", Strings.unquote("'dude'"));
        Assert.assertEquals("dude'", Strings.unquote("dude'"));
        Assert.assertEquals("\"dude'", Strings.unquote("\"dude'"));
        Assert.assertEquals("dude", Strings.unquote("`dude`"));
        Assert.assertEquals("yo 'dude' man", Strings.unquote("yo 'dude' man"));
    }

    @Test
    public void join() {
        Assert.assertEquals(null, Strings.join('.'));
        Assert.assertEquals("man", Strings.join('.', "man"));
        Assert.assertEquals("dude/man", Strings.join('/', "dude", "man"));
    }

    @Test
    public void joinEmptyString() {
        Assert.assertEquals(null, Strings.join('.'));
        Assert.assertEquals("dude/man", Strings.join('/', "dude", "", "man", ""));
    }

    @Test
    public void unquoteDatabaseName() {
        Assert.assertEquals("man", Strings.unquoteDatabaseName("`man`"));
        Assert.assertEquals("dude.man", Strings.unquoteDatabaseName("dude.`man`"));
        Assert.assertEquals("dude.man", Strings.unquoteDatabaseName("'dude'.man"));
    }

    @Test
    public void fixInternalClassName() {
        Assert.assertEquals("org/apache/commons/Dude",
                Strings.fixInternalClassName("com/newrelic/agent/deps/org/apache/commons/Dude"));
    }

    @Test
    public void fixInternalClassNameNoSlashes() {
        Assert.assertEquals("org/test/Dude", Strings.fixInternalClassName("org.test.Dude"));
    }

}
