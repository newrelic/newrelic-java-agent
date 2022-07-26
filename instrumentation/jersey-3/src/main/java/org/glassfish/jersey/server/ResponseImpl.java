/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.glassfish.jersey.server;

import com.newrelic.api.agent.HeaderType;

public class ResponseImpl implements com.newrelic.api.agent.Response {

    private final ContainerResponse response;

    public ResponseImpl(ContainerResponse containerResponse) {
        this.response = containerResponse;
    }

    @Override
    public int getStatus() throws Exception {
        return response.getStatus();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return response.getStatusInfo().getReasonPhrase();
    }

    @Override
    public void setHeader(String name, String value) {
        response.getHeaders().putSingle(name, value);
    }

    @Override
    public String getContentType() {
        return response.getStringHeaders().getFirst("Content-Type");
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}