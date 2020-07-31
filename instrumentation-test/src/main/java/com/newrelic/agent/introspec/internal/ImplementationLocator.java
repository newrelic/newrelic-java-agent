/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.weavepackage.WeavePostprocessor;
import org.junit.runners.model.InitializationError;

import com.newrelic.agent.introspec.Introspector;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;
import com.newrelic.weave.weavepackage.WeavePreprocessor;

import java.util.Map;

public class ImplementationLocator {
    public static Introspector createIntrospector(Map<String, Object> config) {
        return IntrospectorImpl.createIntrospector(config);
    }

    public static Class<?> loadWithWeavingClassLoader(Class<?> classUnderTest) throws InitializationError {
        return TransformingTestUtils.applyClassLoader(classUnderTest, new WeavingClassLoader(
                TransformingTestUtils.getParentAsUrlClassLoader(),
                WeaveIncludes.readWeaveTestConfigIncludePrefixes(classUnderTest),
                ErrorTrapHandler.NO_ERROR_TRAP_HANDLER, WeavePreprocessor.NO_PREPROCESSOR,
                WeavePostprocessor.NO_POSTPROCESSOR));
    }

    public static Class<?> loadWithInstrumentingClassLoader(Class<?> classUnderTest, Map<String, Object> configOverrides) throws Exception {
        createIntrospector(configOverrides);
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        return TransformingTestUtils.applyClassLoader(classUnderTest, new InstrumentingClassLoader(
                TransformingTestUtils.getParentAsUrlClassLoader(), WeaveIncludes.readWeaveTestConfigIncludePrefixes(
                        classUnderTest), agentConfig));
    }
}
