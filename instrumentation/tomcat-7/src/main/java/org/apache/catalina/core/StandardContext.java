/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import java.util.logging.Level;

import javax.servlet.ServletRequestListener;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.tomcat7.TomcatServletRequestListener;

@Weave
public abstract class StandardContext {

    protected synchronized void startInternal() {
        try {
            ServletRequestListener listener = new TomcatServletRequestListener();
            addApplicationEventListener(listener);
            NewRelic.getAgent().getLogger().log(Level.FINER, "Registered ServletRequestListener for {0}",
                    this.getClass());
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Error registering ServletRequestListener for {0}",
                    this.getClass());
        }

        Weaver.callOriginal();
    }

    public abstract void addApplicationEventListener(Object listener);
}
