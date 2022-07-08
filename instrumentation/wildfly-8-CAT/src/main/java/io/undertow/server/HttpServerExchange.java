/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.undertow.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/*
 * The CAT point cut for wildfly had to be pulled out because 
 * the jar was not getting loaded correctly.
 * 
 * com.newrelic.instrumentation.wildfly-8 FINER: Skipping com.newrelic.instrumentation.wildfly-8 instrumentation.  
 * Unresolved classes: [io/undertow/servlet/spec/HttpServletRequestImpl, 
 * io/undertow/servlet/core/DeploymentManagerImpl]
 */
@Weave
public abstract class HttpServerExchange {

    io.undertow.server.HttpServerExchange startResponse() {
        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        return Weaver.callOriginal();
    }

}
