/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

public class SpanError {
    private Class<?> errorClass;
    private String errorMessage;
    private Integer errorStatus;
    private boolean expectedError;

    public Class<?> getErrorClass() {
        return errorClass;
    }

    public void setErrorClass(Class<?> errorClass) {
        this.errorClass = errorClass;
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
