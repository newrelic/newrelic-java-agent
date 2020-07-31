/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import java.util.Arrays;
import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jboss7.TomcatServletRequestListener;

@Weave
public abstract class StandardContext extends ContainerBase {

    public void setApplicationEventListeners(Object listeners[]) {

        NewRelic.getAgent().getLogger().log(Level.FINER, "Registering request listener for {0}", this);
        if (listeners != null) {
            listeners = Arrays.copyOf(listeners, listeners.length + 1);

            listeners[listeners.length - 1] = new TomcatServletRequestListener();
        }

        Weaver.callOriginal();
    }

    public abstract String getPath();
}
