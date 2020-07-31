/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.websphere;

import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class ExtendedResponseWrapper implements Response {

    private final IExtendedResponse extendedResponse;

    public ExtendedResponseWrapper(IExtendedResponse extendedResponse) {
        this.extendedResponse = extendedResponse;
    }

    @Override
    public int getStatus() throws Exception {
        return extendedResponse.getStatusCode();
    }

    @Override
    public String getStatusMessage() throws Exception {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {
        extendedResponse.setHeader(name, value, false);
    }

    @Override
    public String getContentType() {
        return extendedResponse.getContentType();
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }
}
