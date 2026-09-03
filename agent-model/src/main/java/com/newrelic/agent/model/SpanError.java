/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public class SpanError {
    private String errorClassName;
    private String errorMessage;
    private Integer errorStatus;
    private boolean expectedError;

    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClass) {
        this.errorClassName = errorClass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getErrorStatus() {
        return errorStatus;
    }

    public void setErrorStatus(Integer errorStatus) {
        this.errorStatus = errorStatus;
    }

    public boolean isExpectedError() {
        return expectedError;
    }

    public void setExpectedError(boolean expectedError) {
        this.expectedError = expectedError;
    }
}
