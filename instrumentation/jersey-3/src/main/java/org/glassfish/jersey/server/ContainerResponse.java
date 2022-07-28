/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.glassfish.jersey.server;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class ContainerResponse {

    @NewField
    private int contentLength;

    ContainerResponse(ContainerRequest requestContext, OutboundJaxrsResponse response) {
        if (response != null) {
            this.contentLength = response.getLength();
        }
        NewRelic.setRequestAndResponse(new RequestImpl(requestContext), new ResponseImpl(this));
    }

    public abstract int getStatus();

    public abstract MultivaluedMap<String, Object> getHeaders();

    public abstract MultivaluedMap<String, String> getStringHeaders();

    public abstract Response.StatusType getStatusInfo();

    public void close() {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx != null) {
            tx.getCrossProcessState().processOutboundResponseHeaders(new ResponseImpl(this), contentLength);
        }
        // Optimization. This allows the weaver to clear the backing NewField map
        contentLength = 0;
        Weaver.callOriginal();
    }
}
