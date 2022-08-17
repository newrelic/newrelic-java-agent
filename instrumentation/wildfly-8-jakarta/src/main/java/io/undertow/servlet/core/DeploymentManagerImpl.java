/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.undertow.servlet.core;

import com.nr.agent.instrumentation.wildfly8.WildflyUtil;
import io.undertow.servlet.api.ListenerInfo;

import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.wildfly8.WildflyServletRequestListener;

@Weave(type = MatchType.ExactClass)
public abstract class DeploymentManagerImpl {

    private ApplicationListeners createListeners() {
        WildflyUtil.setServerInfo();
        ApplicationListeners listeners = Weaver.callOriginal();
        listeners.addListener(new ManagedListener(new ListenerInfo(WildflyServletRequestListener.class), false));
        NewRelic.getAgent().getLogger().log(Level.FINER, "Registering request listener for {0} ", this);

        return listeners;
    }

}
