/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cli;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AgentCommandLineParserTest {
    @Test
    public void variousHelpCases() {
        AgentCommandLineParser target = new AgentCommandLineParser();

        for (String testCase : new String[] { "-h", "--help", "-help", "help", "obscure" }) {
            assertEquals(1, target.parseCommand(new String[] { testCase }));
        }
    }

    @Test
    public void variousVersionCases() {
        AgentCommandLineParser target = new AgentCommandLineParser();
        for (String testCase : new String[] { "version", "-v", "--version", "-version", "version a b c" }) {
            assertEquals(0, target.parseCommand(testCase.split(" ")));
        }
    }

    @Test
    public void noArgsTests() {
        AgentCommandLineParser target = new AgentCommandLineParser();
        assertEquals(1, target.parseCommand(null));
        assertEquals(1, target.parseCommand(new String[0]));
        assertEquals(1, target.parseCommand(new String[] { null }));
        assertEquals(1, target.parseCommand(new String[] { "" }));
    }

    @Test
    public void badCommandTest() {
        AgentCommandLineParser target = new AgentCommandLineParser();
        assertEquals(1, target.parseCommand(new String[] { "glarb" }));
    }
}