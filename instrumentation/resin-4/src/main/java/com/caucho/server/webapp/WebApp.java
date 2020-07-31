/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.webapp;

import java.util.EventListener;
import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.resin4.NRServletRequestListener;

@Weave
public abstract class WebApp {

    public void init() {

        final NRServletRequestListener requestListener = new NRServletRequestListener();
        NewRelic.getAgent().getLogger().log(Level.FINE, "Registering Resin request listener");
        addListener(requestListener);

        Weaver.callOriginal();
    }

    public void addListener(EventListener listener) {
        Weaver.callOriginal();
    }

    public abstract String getContextPath();
}
