/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.webcontainer.webapp;

import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.websphere.NRServletRequestListener;

@Weave
public abstract class WebAppImpl extends WebApp  {

    protected void registerWebAppListeners() {

        NewRelic.getAgent().getLogger().log(Level.INFO, "Registering WebSphere request listener");
        try {
            addListener(new NRServletRequestListener());
        } catch (Exception ex) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE, "Unable to register WebSphere request listener");
            NewRelic.getAgent().getLogger().log(Level.FINEST, ex, ex.toString());
        }

        Weaver.callOriginal();

    }

    /**
     * We weave this method so that the class/method matcher has a reasonable method to match against.
     * 
     * @return
     */
    public String getServerInfo() {
        return Weaver.callOriginal();
    }
}
