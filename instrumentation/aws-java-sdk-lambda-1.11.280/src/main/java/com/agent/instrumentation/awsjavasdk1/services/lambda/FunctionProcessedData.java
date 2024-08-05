/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

/**
 * Function data extracted from the request and config.
 */
class FunctionProcessedData {
    private final String functionName;
    private final String arn;

    public FunctionProcessedData(String functionName, String arn) {
        this.functionName = functionName;
        this.arn = arn;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getArn() {
        return arn;
    }
}
