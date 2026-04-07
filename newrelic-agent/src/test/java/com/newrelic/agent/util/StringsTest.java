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

    @Test
    public void replaceDotHyphenWithUnderscore() {
        Assert.assertNull(Strings.replaceDotHyphenWithUnderscore(null));
        Assert.assertEquals("", Strings.replaceDotHyphenWithUnderscore(""));
        Assert.assertEquals("string without dot OR hyphen",
                Strings.replaceDotHyphenWithUnderscore("string without dot OR hyphen"));

        Assert.assertEquals("string_with_dots_AND_hyphens",
                Strings.replaceDotHyphenWithUnderscore("string.with-dots-AND.hyphens"));

        Assert.assertEquals("___string_with_dots_AND_hyphens_at_ends__",
                Strings.replaceDotHyphenWithUnderscore("-.-string.with-dots-AND.hyphens.at_ends.-"));
    }

    @Test
    public void obfuscate() {
        Assert.assertNull(Strings.obfuscate(null));
        Assert.assertEquals("f", Strings.obfuscate("f"));
        Assert.assertEquals("fo", Strings.obfuscate("fo"));
        Assert.assertEquals("foo", Strings.obfuscate("foo"));
        Assert.assertEquals("b***r", Strings.obfuscate("barbar"));
        Assert.assertEquals("l***g", Strings.obfuscate("long-long-string"));
    }
}
