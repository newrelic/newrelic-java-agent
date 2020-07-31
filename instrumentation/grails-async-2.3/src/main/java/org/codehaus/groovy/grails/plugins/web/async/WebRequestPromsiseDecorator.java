/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.codehaus.groovy.grails.plugins.web.async;

import groovy.lang.Closure;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class WebRequestPromsiseDecorator {

    @Trace(dispatcher = true)
    public Object invokeClosure(Closure c, Object args) {
        AgentBridge.getAgent().startAsyncActivity(c);
        return Weaver.callOriginal();
    }
}
