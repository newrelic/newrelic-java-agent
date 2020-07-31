/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.introspec.ErrorEvent;

class ErrorEventImpl extends EventImpl implements ErrorEvent {

    private String errorClass;
    private String errorMessage;
    private String txName;

    public ErrorEventImpl(com.newrelic.agent.model.ErrorEvent event) {
        super(event.getType(), event.getUserAttributesCopy());
        errorClass = event.getErrorClass();
        errorMessage = event.getErrorMessage();
        txName = event.getTransactionName();
    }

    @Override
    public String getErrorClass() {
        return errorClass;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getTransactionName() {
        return txName;
    }

}
