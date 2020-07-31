/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.profile.ProfilerParameters;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.profile.StartProfilerCommand;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.rpm.RPMConnectionServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.util.JSONException;
import com.newrelic.agent.util.TimeConversion;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandParserThreadProfilerTest {

    private CommandParser commandParser;
    private MockCoreService agentControl;
    private ProfilerParameters profilerParameters;
    private ProfilerService profilerService;

    private MockServiceManager createServiceManager(Map<String, Object> config) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ThreadService threadService = new ThreadService();
        serviceManager.setThreadService(threadService);

        ConfigService configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(
                config), config);
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
        profilerService = new ProfilerService() {

            @Override
            public void startProfiler(ProfilerParameters parameters) {
                profilerParameters = parameters;
            }

            @Override
            public int stopProfiler(Long profileId, boolean report) {
                return 0;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

        };

        commandParser = new CommandParser();
        commandParser.doStart();
        commandParser.addCommands(new ShutdownCommand(agentControl), new RestartCommand());
    }

    @Test
    public void testProfilerCommandParserLegacyNullDefault() throws Exception {
        final long id = 21003L;
        final long profileId = 2867607L;
        final double samplePeriod = 0.1;
        final double duration = 300.0;
        final boolean onlyRunnableThreads = false;
        final boolean onlyRequestThreads = false;
        // Parse JSON string and create thread profiler command map.
        final String threadProfilerGetCommand = createJSONTPv1(id, profileId, samplePeriod, duration,
                onlyRunnableThreads, onlyRequestThreads, null);

        // Parse JSON string and process thread profiler command map.
        Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error processing thread profiler commands", commands instanceof Map);
        Map<?, ?> commandMap = Map.class.cast(commands);

        Assert.assertTrue("Error processing thread profiler commands", commandMap == Collections.EMPTY_MAP);
        Assert.assertEquals("Error processing thread profiler command profile_agent_code", false,
                profilerParameters.isProfileAgentThreads());
        Assert.assertEquals("Error processing thread profiler command only_runnable_threads", onlyRunnableThreads,
                profilerParameters.isRunnablesOnly());
        Assert.assertEquals("Error processing thread profiler command sample_period",
                TimeConversion.convertSecondsToMillis(samplePeriod),
                profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command duration", TimeConversion.convertSecondsToMillis(
                duration), profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command profiler_format", null,
                profilerParameters.getProfilerFormat());
    }

    @Test
    public void testProfilerCommandParserProfileAgent() throws Exception {
        final long id = 200L;
        final long profileId = 300L;
        final double samplePeriod = 20;
        final double duration = 500.0;
        final boolean onlyRunnableThreads = false;
        final boolean onlyRequestThreads = true;
        final boolean profileAgentCode = false;
        // Parse JSON string and create thread profiler command map.
        final String threadProfilerGetCommand = createJSONTPv1(id, profileId, samplePeriod, duration,
                onlyRunnableThreads, onlyRequestThreads, profileAgentCode);

        // Parse JSON string and process thread profiler command map.
        Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error processing thread profiler commands", commands instanceof Map);
        Map<?, ?> commandMap = Map.class.cast(commands);

        Assert.assertTrue("Error processing thread profiler commands", commandMap == Collections.EMPTY_MAP);
        Assert.assertEquals("Error processing thread profiler command profile_agent_code", profileAgentCode,
                profilerParameters.isProfileAgentThreads());
        Assert.assertEquals("Error processing thread profiler command only_runnable_threads", onlyRunnableThreads,
                profilerParameters.isRunnablesOnly());
        Assert.assertEquals("Error processing thread profiler command sample_period",
                TimeConversion.convertSecondsToMillis(samplePeriod),
                profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command duration", TimeConversion.convertSecondsToMillis(
                duration), profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command profiler_format", null,
                profilerParameters.getProfilerFormat());
    }

    @Test
    public void testProfilerCommandParserLegacyMinimumRequired() throws Exception {
        final long id = 21003L;
        final long profileId = 2867607L;
        final double samplePeriod = 0.1;
        final double duration = 300.0;

        // Parse JSON string and create thread profiler command map.
        final String threadProfilerGetCommand = createJSONTPv1Minimum(id, profileId, samplePeriod, duration);

        // Parse JSON string and process thread profiler command map.
        Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error processing thread profiler commands", commands instanceof Map);
        Map<?, ?> commandMap = Map.class.cast(commands);

        Assert.assertTrue("Error processing thread profiler commands", commandMap == Collections.EMPTY_MAP);
        Assert.assertEquals("Error processing thread profiler command profile_agent_code", false,
                profilerParameters.isProfileAgentThreads());
        Assert.assertEquals("Error processing thread profiler command only_runnable_threads", false,
                profilerParameters.isRunnablesOnly());
        Assert.assertEquals("Error processing thread profiler command sample_period",
                TimeConversion.convertSecondsToMillis(samplePeriod),
                profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command duration", TimeConversion.convertSecondsToMillis(
                duration), profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command profiler_format", null,
                profilerParameters.getProfilerFormat());
    }

    @Test
    public void testProfilerCommandParserInvalidPeriod() throws Exception {
        final long id = 2L;
        final long profileId = 2L;
        final double samplePeriod = 500.0;
        final double duration = 300.0;

        // Parse JSON string and create thread profiler command map.
        final String threadProfilerGetCommand = createJSONTPv1Minimum(id, profileId, samplePeriod, duration);
        // Parse JSON string and process thread profiler command map.
        Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error processing thread profiler commands", commands instanceof JSONException);
        Assert.assertTrue("Error processing thread profiler commands",
                ((JSONException) commands).getCause() instanceof CommandException);
    }

    @Test
    public void testProfilerCommandParserCommandException() throws Exception {
        final long id = 21003L;

        // Parse JSON string and create thread profiler command map.
        final String threadProfilerGetCommand = createJSONNoArguments(id);

        // Parse JSON string and process thread profiler command map.
        Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error handling invalid thread profiler command", commands instanceof JSONException);
        Assert.assertTrue("Error handling invalid thread profiler command",
                ((JSONException) commands).getCause() instanceof CommandException);
    }

    @Test
    public void testProfilerCommandParserTPv2() throws Exception {
        final long id = 1L;
        final long profileId = 22L;
        final double samplePeriod = 10.3;
        final double duration = 500.0;
        final boolean onlyRunnableThreads = true;
        final boolean onlyRequestThreads = true;
        final boolean profileAgentCode = false;
        final String profilerFormat = "v2";
        final boolean profileInstrumentation = false;
        // Parse JSON string and create thread profiler command map.
        final String threadProfilerGetCommand = createJSONTPV2(id, profileId, samplePeriod, duration, onlyRunnableThreads,
                onlyRequestThreads, profileAgentCode, profilerFormat, profileInstrumentation);
        final Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        final Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error processing thread profiler commands", commands instanceof Map);
        Map<?, ?> commandMap = Map.class.cast(commands);

        Assert.assertTrue("Error processing thread profiler command", commandMap == Collections.EMPTY_MAP);
        Assert.assertEquals("Error processing thread profiler command profile_agent_code", profileAgentCode,
                profilerParameters.isProfileAgentThreads());
        Assert.assertEquals("Error processing thread profiler command only_runnable_threads", onlyRunnableThreads,
                profilerParameters.isRunnablesOnly());
        Assert.assertEquals("Error processing thread profiler command only_runnable_threads", profileInstrumentation,
                profilerParameters.isProfileInstrumentation());
        Assert.assertEquals("Error processing thread profiler command sample_period",
                TimeConversion.convertSecondsToMillis(samplePeriod),
                profilerParameters.getSamplePeriodInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command duration", TimeConversion.convertSecondsToMillis(
                duration), profilerParameters.getDurationInMillis().longValue());
        Assert.assertEquals("Error processing thread profiler command profiler_format", profilerFormat,
                profilerParameters.getProfilerFormat());
    }

    @Test
    public void testProfilerCommandParserTPv2NullDefaults() throws Exception {
        final long id = 21003L;
        final long profileId = 2867607L;
        final double samplePeriod = 0.3;
        final double duration = 50.99;
        final String profilerFormat = "v2";
        final String threadProfilerGetCommand = createJSONTPV2(id, profileId, samplePeriod, duration, null, null, null,
                profilerFormat, null);
        // Parse JSON string and process thread profiler command map.
        Map<Long, Object> result = processThreadProfilerCommand(threadProfilerGetCommand);

        Assert.assertFalse("Error processing thread profiler commands", result.isEmpty());
        Object commands = result.get(id);
        Assert.assertNotNull(commands);
        Assert.assertTrue("Error processing thread profiler commands", commands instanceof Map);
        Map<?, ?> commandMap = Map.class.cast(commands);

        Assert.assertTrue("Error processing thread profiler commands", commandMap == Collections.EMPTY_MAP);
        Assert.assertFalse(profilerParameters.isProfileAgentThreads());
        Assert.assertFalse(profilerParameters.isRunnablesOnly());
        Assert.assertTrue(profilerParameters.isProfileInstrumentation());
        Assert.assertTrue("Error processing thread profiler command: sample_period",
                TimeConversion.convertSecondsToMillis(samplePeriod) ==
                profilerParameters.getSamplePeriodInMillis());
        Assert.assertTrue("Error processing thread profiler command: duration", TimeConversion.convertSecondsToMillis(
                duration) == 
                profilerParameters.getDurationInMillis());
        Assert.assertEquals("v2", profilerParameters.getProfilerFormat());
    }

    private Map<Long, Object> processThreadProfilerCommand(String threadProfilerGetCommand) throws Exception {
        JSONParser parser = new JSONParser();
        Object parsedTPAgentCommand = parser.parse(threadProfilerGetCommand);
        Map<?, ?> tpAgentCommandMap = Map.class.cast(parsedTPAgentCommand);
        List<List<?>> threadProfilerCommands = (List<List<?>>) tpAgentCommandMap.get("return_value");

        commandParser.addCommands(new StartProfilerCommand(profilerService));
        MockRPMService rpmService = new MockRPMService();
        Map<Long, Object> result = commandParser.processCommands(rpmService, threadProfilerCommands);
        return result;
    }

    private String createJSONTPv1(Long id, Long profileID, Double samplePeriod, Double duration,
            Boolean onlyRunnableThreads, Boolean requestThreads, Boolean profileAgent) {
        return "{\"return_value\":[[" + id + ",{\"name\":\"start_profiler\",\"arguments\":{"
                + "\"profile_id\":" + profileID + ",\"sample_period\":" + samplePeriod
                + ",\"duration\":" + duration + ",\"only_runnable_threads\":"
                + (null != onlyRunnableThreads ? onlyRunnableThreads.toString() : "null") + ",\"only_request_threads\":"
                + (null != requestThreads ? requestThreads.toString() : "null") + ",\"profile_agent_code\":"
                + (null != profileAgent ? profileAgent.toString() : "null") + "}}]]}";
    }

    private String createJSONTPv1Minimum(Long id, Long profileID, Double samplePeriod, Double duration) {
        return "{\"return_value\":[[" + id + ",{\"name\":\"start_profiler\",\"arguments\":{"
                + "\"profile_id\":" + profileID + ",\"sample_period\":" + samplePeriod
                + ",\"duration\":" + duration + "}}]]}";
    }

    private String createJSONNoArguments(Long id) {
        return "{\"return_value\":[[" + id + ",{\"name\":\"start_profiler\"}]]}";
    }

    private String createJSONTPV2(Long id, Long profileID, Double samplePeriod, Double duration,
            Boolean onlyRunnableThreads, Boolean requestThreads, Boolean profileAgent, String profilerVersion, Boolean profileInstrumentation){
        return "{\"return_value\":[[" + id
               + ",{\"name\":\"start_profiler\",\"arguments\":{"
               + "\"profile_id\":" + profileID
               + ",\"sample_period\":" + samplePeriod
               + ",\"duration\":" + duration + ",\"only_runnable_threads\":"
               + (null != onlyRunnableThreads ? onlyRunnableThreads.toString() : "null") + ",\"only_request_threads\":"
               + (null != requestThreads ? requestThreads.toString() : "null") + ",\"profile_agent_code\":"
               + (null != profileAgent ? profileAgent.toString() : "null") 
               + ",\"profiler_format\":\"" 
               + profilerVersion 
               + "\",\"profile_instrumentation\":" + (null != profileInstrumentation ? profileInstrumentation.toString() : "null") 
               + "}}]]}";
    }
}
