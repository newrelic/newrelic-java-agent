/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.rpm.RPMConnectionServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class CommandParserTest {

    private CommandParser commandParser;
    private MockCoreService agentControl;

    private MockServiceManager createServiceManager(Map<String, Object> config) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config), config);
        serviceManager.setConfigService(configService);

        TransactionService transactionService = new TransactionService();
        serviceManager.setTransactionService(transactionService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        RPMConnectionService rpmConnectionService = new RPMConnectionServiceImpl();
        serviceManager.setRPMConnectionService(rpmConnectionService);

        ProfilerService profilerService = new ProfilerService();
        serviceManager.setProfilerService(profilerService);

        EnvironmentService envService = new EnvironmentServiceImpl();
        serviceManager.setEnvironmentService(envService);

        HarvestService harvestService = new HarvestServiceImpl();
        serviceManager.setHarvestService(harvestService);

        return serviceManager;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        AgentHelper.initializeConfig();
    }

    @Before
    public void setup() throws Exception {
        agentControl = new MockCoreService();
        createServiceManager(new HashMap<String, Object>());
        commandParser = new CommandParser();
        commandParser.doStart();
        commandParser.addCommands(new ShutdownCommand(agentControl), new RestartCommand());
    }

    @Test(expected = UnknownCommand.class)
    public void unknownCommand() throws UnknownCommand {
        commandParser.getCommand("dude");
    }

    @Test
    public void shutdown() {
        MockRPMService rpmService = new MockRPMService();
        for (int i = 0; i < 5; i++) {
            List<List<?>> commands = new ArrayList<>();
            commands.add(createCommand(1, ShutdownCommand.COMMAND_NAME));
            commandParser.processCommands(rpmService, commands);
        }

        Assert.assertEquals(0, rpmService.getRestartCount());
        Assert.assertEquals(5, agentControl.getShutdownCount());
    }

    @Test
    public void restart() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        MockRPMService rpmService = new MockRPMService();
        List<List<?>> commands = new ArrayList<>();
        commands.add(createCommand(1, RestartCommand.COMMAND_NAME));
        commandParser.processCommands(rpmService, commands);

        Assert.assertEquals(1, rpmService.getRestartCount());
        Assert.assertEquals(0, agentControl.getShutdownCount());
    }

    private List<?> createCommand(int id, final String name) {
        return createCommand(id, name, null);
    }

    private List<?> createCommand(int id, final String name, final Map<?, ?> args) {
        Map<String, Object> command = new HashMap<>();
        command.put("name", name);
        if (args != null) {
            command.put("arguments", args);
        }
        return Arrays.asList(id, command);
    }

    @Test
    public void testDisallowedShutdownCommand() throws Exception {
        createServiceManager(ImmutableMap.<String, Object>of("command_parser",
                ImmutableMap.of("disallow", "shutdown")));

        agentControl = new MockCoreService();
        commandParser = new CommandParser();
        commandParser.doStart();
        commandParser.addCommands(new ShutdownCommand(agentControl), new RestartCommand());

        MockRPMService rpmService = new MockRPMService();
        for (int i = 0; i < 5; i++) {
            List<List<?>> commands = new ArrayList<>();
            commands.add(createCommand(1, ShutdownCommand.COMMAND_NAME));
            commandParser.processCommands(rpmService, commands);
        }

        Assert.assertEquals(0, rpmService.getRestartCount());
        Assert.assertEquals(5, agentControl.getShutdownCount());
    }

    @Test
    public void testUpdateDisallowedRestartCommand() throws Exception {
        // First test without the disallow
        MockRPMService rpmService = new MockRPMService();
        for (int i = 0; i < 5; i++) {
            List<List<?>> commands = new ArrayList<>();
            commands.add(createCommand(1, RestartCommand.COMMAND_NAME));
            commandParser.processCommands(rpmService, commands);
        }

        Assert.assertEquals(5, rpmService.getRestartCount());
        Assert.assertEquals(0, agentControl.getShutdownCount());
        
        // Second test with the disallow list (multiple values)
        agentControl = new MockCoreService();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(ImmutableMap.<String, Object>of("command_parser",
                ImmutableMap.of("disallow", "other_command, restart,some_other_command")));
        commandParser.configChanged(null, agentConfig);

        rpmService = new MockRPMService();
        for (int i = 0; i < 5; i++) {
            List<List<?>> commands = new ArrayList<>();
            commands.add(createCommand(1, RestartCommand.COMMAND_NAME));
            commandParser.processCommands(rpmService, commands);
        }

        Assert.assertEquals(5, rpmService.getRestartCount());
        Assert.assertEquals(0, agentControl.getShutdownCount());
    }

    @Test
    public void testDisallowedPingCommand() throws Exception {
        createServiceManager(ImmutableMap.<String, Object>of("command_parser",
                ImmutableMap.of("disallow", PingCommand.COMMAND_NAME)));

        agentControl = new MockCoreService();
        commandParser = new CommandParser();
        commandParser.doStart();
        commandParser.addCommands(new ShutdownCommand(agentControl), new RestartCommand());

        MockRPMService rpmService = new MockRPMService();
        for (int i = 0; i < 5; i++) {
            List<List<?>> commands = new ArrayList<>();
            commands.add(createCommand(1, PingCommand.COMMAND_NAME));
            Map<Long, Object> commandResult = commandParser.processCommands(rpmService, commands);
            Assert.assertEquals(0, commandResult.size());
        }
    }
}
