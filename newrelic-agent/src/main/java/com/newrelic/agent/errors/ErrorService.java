/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.service.EventService;
import com.newrelic.agent.transaction.TransactionThrowable;

import java.util.List;
import java.util.Map;

public interface ErrorService extends EventService {

    List<TracedError> getAndClearTracedErrors();

    void reportErrors(TracedError... tracedErrors);

    void reportError(TracedError error);

    void reportHTTPError(String message, int statusCode, String uri);

    void reportException(Throwable throwable);

    void reportException(Throwable throwable, Map<String, ?> params, boolean expected);

    void reportError(String message, Map<String, ?> params, boolean expected);

    void addHarvestableToService();
}
