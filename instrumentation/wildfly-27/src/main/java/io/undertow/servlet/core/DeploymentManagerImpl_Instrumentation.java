/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.undertow.servlet.core;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.wildfly27.WildflyServletRequestListener;
import com.nr.agent.instrumentation.wildfly27.WildflyUtil;
import io.undertow.servlet.api.ListenerInfo;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "io.undertow.servlet.core.DeploymentManagerImpl")
public abstract class DeploymentManagerImpl_Instrumentation {

    private ApplicationListeners createListeners() {
        WildflyUtil.setServerInfo();
        ApplicationListeners listeners = Weaver.callOriginal();
        listeners.addListener(new ManagedListener(new ListenerInfo(WildflyServletRequestListener.class), false));
        NewRelic.getAgent().getLogger().log(Level.FINER, "Registering request listener for {0} ", this);

        return listeners;
    }

}
