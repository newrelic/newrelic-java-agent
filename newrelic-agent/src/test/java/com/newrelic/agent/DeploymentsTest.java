/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import java.util.Map;

import com.newrelic.test.marker.RequiresFork;
import org.junit.Assert;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.junit.Test;

import com.newrelic.agent.config.AgentConfigFactory;
import com.newrelic.agent.config.AgentConfigFactoryTest;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AgentConfig;
import org.junit.experimental.categories.Category;

@Category(RequiresFork.class)
public class DeploymentsTest {

    private MockServiceManager serviceManager;

    @Test(expected = IllegalArgumentException.class)
    public void noAppDeployment() throws Exception {
        CommandLine cmd = getCommandLine();

        Deployments.recordDeployment(cmd, getStagingAgentConfig());
    }

    // @Test
    public void deployment() throws Exception {
        CommandLine cmd = getCommandLine("-appname", "BenderUnitTest");

        // Assert.assertEquals(204, Deployments.recordDeployment(cmd, getStagingAgentConfig()));
        Assert.assertEquals(404, Deployments.recordDeployment(cmd, getStagingAgentConfig()));
    }

    // @Test
    public void deploymentWithDescription() throws Exception {
        CommandLine cmd = getCommandLine("deployment", "Oh dude!  Describe this thing", "-appname", "BenderUnitTest");
        // Assert.assertEquals(204, Deployments.recordDeployment(cmd, getStagingAgentConfig()));
        Assert.assertEquals(404, Deployments.recordDeployment(cmd, getStagingAgentConfig()));
    }

    // @Test
    public void deploymentWithMultivalueAppName() throws Exception {
        CommandLine cmd = getCommandLine("deployment", "Deploy to first name in the semicolon-delimited list");
        // Assert.assertEquals(204, Deployments.recordDeployment(cmd, getStagingMultiNameAgentConfig()));
        Assert.assertEquals(404, Deployments.recordDeployment(cmd, getStagingMultiNameAgentConfig()));
    }

    private AgentConfig getStagingMultiNameAgentConfig() throws Exception {
        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        settings.put(AgentConfigImpl.APP_NAME, "BenderUnitTest;AllYourTestAreBelongToUs");
        return AgentConfigFactory.createAgentConfig(settings, null, null);
    }

    private AgentConfig getStagingAgentConfig() throws Exception {
        Map<String, Object> settings = AgentConfigFactoryTest.createStagingMap();
        settings.put(AgentConfigImpl.APP_NAME, null);
        return AgentConfigFactory.createAgentConfig(settings, null, null);
    }

    private CommandLine getCommandLine(String... args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        return parser.parse(AgentCommandLineParser.getCommandLineOptions(), args);
    }
}
