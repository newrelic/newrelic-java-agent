/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public class NoOpWebResponse implements WebResponse {
    public static final WebResponse INSTANCE = new NoOpWebResponse();

    @Override
    public void setStatus(int statusCode) {
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public void setStatusMessage(String message) {
    }

    @Override
    public String getStatusMessage() {
        return "";
    }

    @Override
    public void freezeStatus() {
    }

}
