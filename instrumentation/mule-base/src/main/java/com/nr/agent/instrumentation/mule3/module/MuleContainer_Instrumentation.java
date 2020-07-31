/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3.module;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * Provides serverInfo. This needs to run before the woven method so that Jetty Server doesn't start and set the
 * dispatcher first. However, we do let Jetty set the host and port.
 */
@Weave(type = MatchType.ExactClass, originalName = "org.mule.module.launcher.MuleContainer")
public abstract class MuleContainer_Instrumentation {

    public void start(boolean registerShutdownHook) {
        AgentBridge.publicApi.setServerInfo("Mule", ManifestUtils.getVersionFromManifest(getClass(), "Mule", "3.x"));
        Weaver.callOriginal();
    }

}
