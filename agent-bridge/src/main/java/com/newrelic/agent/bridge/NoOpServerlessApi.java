/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

public class NoOpServerlessApi implements ServerlessApi {

    @Override
    public void setArn(String arn) {
        // No-op
    }

    @Override
    public void setFunctionVersion(String functionVersion) {
        // No-op
    }

    @Override
    public String getArn() {
        return null;
    }

    @Override
    public String getFunctionVersion() {
        return null;
    }
}
