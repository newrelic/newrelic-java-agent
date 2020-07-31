/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.webapp;

import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.resin3.NRServletRequestListener;

@Weave
public abstract class WebApp {

    public void init() {

        final NRServletRequestListener requestListener = new NRServletRequestListener();
        Listener listener = new Listener() {
            @Override
            public Object createListenerObject() throws java.lang.Exception {
                return requestListener;
            }
        };
        NewRelic.getAgent().getLogger().log(Level.FINE, "Registering Resin request listener for context: {0}",
                getContextPath());
        addListener(listener);

        Weaver.callOriginal();
    }

    public void addListener(Listener listener) {
        Weaver.callOriginal();
    }

    public abstract String getContextPath();
}
