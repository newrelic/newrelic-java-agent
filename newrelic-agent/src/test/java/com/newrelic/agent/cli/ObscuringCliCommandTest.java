/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObscuringCliCommandTest {
    private PrintStream savedOut;

    @Test
    public void canParseCommandLine() throws ParseException {
        ObscuringCliCommand target = new ObscuringCliCommand();
        DefaultParser parser = new DefaultParser();
        CommandLine parse = parser.parse(target.getOptions(), new String[] { "--obscuring-key", "foobar", "preobfuscation" });
        assertTrue(parse.hasOption("obscuring-key"));
    }

    @Test
    public void obscuresCorrectly() throws Exception {
        ObscuringCliCommand target = new ObscuringCliCommand();
        DefaultParser parser = new DefaultParser();
        CommandLine parse = parser.parse(target.getOptions(), new String[] { "--obscuring-key", "foobar", "preobfuscation" });
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        target.performCommand(parse);
        assertEquals("Fh0KDQMUExwMAxUbCQE=", baos.toString("UTF-8").trim());
    }

    @Test(expected = ParseException.class)
    public void failsWithoutEncodingKey() throws ParseException {
        ObscuringCliCommand target = new ObscuringCliCommand();
        DefaultParser parser = new DefaultParser();
        parser.parse(target.getOptions(), new String[] { "--unknown-key", "foobar", "preobfuscation" });
    }

    @Test(expected = Exception.class)
    public void failsWithoutPlaintext() throws Exception {
        ObscuringCliCommand target = new ObscuringCliCommand();
        DefaultParser parser = new DefaultParser();
        CommandLine parse = parser.parse(target.getOptions(), new String[] { "--obscuring-key", "foobar" });
        target.performCommand(parse);
    }

    @Before
    public void before() {
        savedOut = System.out;
    }

    @After
    public void after() {
        System.setOut(savedOut);
    }
}