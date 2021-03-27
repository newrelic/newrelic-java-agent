/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.api.agent.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.instrument.Instrumentation;
import java.util.logging.Level;

public class BuiltinExtensionTest {
    @Test
    public void testInvokeBuiltinExtension() {
        ServiceFactory.setServiceManager(Mockito.mock(ServiceManager.class));
        ClassTransformerService classTransformerService = Mockito.mock(ClassTransformerService.class);
        Mockito.when(ServiceFactory.getServiceManager().getClassTransformerService()).thenReturn(
                classTransformerService);

        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Mockito.when(classTransformerService.getExtensionInstrumentation()).thenReturn(instrumentation);

        AgentBridge.agent = Mockito.mock(Agent.class);
        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(AgentBridge.agent.getLogger()).thenReturn(logger);

        BuiltinExtension fakeBuiltinExtension = new BuiltinExtension(Mockito.mock(IAgentLogger.class), PremainEntry.class.getSimpleName(),
                PremainEntry.class.getName());
        fakeBuiltinExtension.invokePremainMethod();

        // Verify that our premain class was invoked
        Mockito.verify(logger, Mockito.times(1)).log(Level.INFO, "My cool extension started! {0}", instrumentation);
    }

    private static final class PremainEntry {
        public static void premain(String agentArgs, Instrumentation inst) {
            AgentBridge.getAgent().getLogger().log(Level.INFO, "My cool extension started! {0}", inst);
        }
    }

}
