/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.reinstrument.ReinstrumentResult;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandTest {

    @Test
    public void restart() throws Exception {
        MockRPMService rpmService = new MockRPMService() {

            @Override
            public void reconnect() {
                throw new RuntimeException("restart");
            }
        };
        MockCoreService executor = new MockCoreService();
        Assert.assertEquals(Collections.EMPTY_MAP, new ShutdownCommand(executor).process(rpmService,
                Collections.EMPTY_MAP));
        try {
            new RestartCommand().process(rpmService, Collections.EMPTY_MAP);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("restart", e.getMessage());
        }
    }

    @Test
    public void shutdown() throws Exception {
        MockRPMService rpmService = new MockRPMService();

        MockCoreService executor = new MockCoreService() {

            @Override
            public void shutdownAsync() {
                throw new RuntimeException("shutdown");
            }

        };

        Assert.assertEquals(Collections.EMPTY_MAP, new RestartCommand().process(rpmService, Collections.EMPTY_MAP));
        try {
            new ShutdownCommand(executor).process(rpmService, Collections.EMPTY_MAP);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("shutdown", e.getMessage());
        }
    }

    @Test
    public void disabledCommand_test() throws CommandException {
        RPMService mockRpmService = Mockito.mock(RPMService.class);
        DisabledCommand disabledCommandWithNameOnly = new DisabledCommand("myCmd");
        DisabledCommand disabledCommandWithNameAndErrMsg = new DisabledCommand("myCmd", "is broken");

        Assert.assertEquals("Command \"myCmd\" is disabled", disabledCommandWithNameOnly.process(mockRpmService, null).get("error"));
        Assert.assertEquals("is broken", disabledCommandWithNameAndErrMsg.process(mockRpmService, null).get("error"));
    }

    @Test
    public void pingCommand_test() throws CommandException {
        RPMService mockRpmService = Mockito.mock(RPMService.class);
        PingCommand pingCommand = new PingCommand();
        Assert.assertEquals(0, pingCommand.process(mockRpmService, null).size());
        Assert.assertEquals("ping", pingCommand.getName());
    }

    @Test(expected = CommandException.class)
    public void instrumentUpdate_process_withEmptyArgs_throwsException() throws CommandException {
        RemoteInstrumentationService mockRemoteInstrumentationService = Mockito.mock(RemoteInstrumentationService.class);
        RPMService mockRpmService = Mockito.mock(RPMService.class);
        InstrumentUpdateCommand instrumentUpdateCommand = new InstrumentUpdateCommand(mockRemoteInstrumentationService);

        instrumentUpdateCommand.process(mockRpmService, null);
    }

    @Test
    public void instrumentUpdate_process_withXmlArg_throwsException() throws CommandException {
        Map<String, Object> outerArgsMap = new HashMap<>();
        Map<String, String> xmlArgsMap = new HashMap<>();
        outerArgsMap.put("instrumentation", xmlArgsMap);
        xmlArgsMap.put("config", "<foo />");

        ReinstrumentResult mockReinstrumentResult = Mockito.mock(ReinstrumentResult.class);
        RemoteInstrumentationService mockRemoteInstrumentationService = Mockito.mock(RemoteInstrumentationService.class);
        RPMService mockRpmService = Mockito.mock(RPMService.class);

        Mockito.when(mockRemoteInstrumentationService.processXml("<foo />")).thenReturn(mockReinstrumentResult);
        Mockito.when(mockReinstrumentResult.getStatusMap()).thenReturn(outerArgsMap);

        InstrumentUpdateCommand instrumentUpdateCommand = new InstrumentUpdateCommand(mockRemoteInstrumentationService);

        Map response = instrumentUpdateCommand.process(mockRpmService, outerArgsMap);
        Assert.assertNotNull(response);
    }
}