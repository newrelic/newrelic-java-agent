/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

import com.newrelic.api.agent.ExtendedResponse;
import com.newrelic.api.agent.HeaderType;

class ErrorExtendedResponse extends ExtendedResponse {
    private final int status;
    private final String flowClassName;

    public ErrorExtendedResponse(int status, String flowClassName) {
        this.status = status;
        this.flowClassName = flowClassName;
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getStatusMessage() {
        return "thing to do was " + flowClassName;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public HeaderType getHeaderType() {
        return null;
    }

    @Override
    public void setHeader(String name, String value) {

    }
}
