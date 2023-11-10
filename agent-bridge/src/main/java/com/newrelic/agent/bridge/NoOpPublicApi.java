/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.Map;

import com.newrelic.api.agent.ErrorGroupCallback;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

class NoOpPublicApi implements PublicApi {

    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {

    }

    @Override
    public void noticeError(String message, Map<String, ?> params, boolean expected) {

    }

    @Override
    public void addCustomParameter(String key, Number value) {

    }

    @Override
    public void addCustomParameter(String key, String value) {

    }

    @Override
    public void addCustomParameter(String key, boolean value) {

    }

    @Override
    public void addCustomParameters(Map<String, Object> params) {

    }

    @Override
    public void setUserId(String userId) {

    }

    @Override
    public void setTransactionName(String category, String name) {

    }

    @Override
    public void ignoreTransaction() {

    }

    @Override
    public void ignoreApdex() {

    }

    @Override
    public void setRequestAndResponse(Request request, Response response) {

    }

    @Override
    public String getBrowserTimingHeader() {

        return "";
    }

    @Override
    public String getBrowserTimingHeader(String nonce) {

        return "";
    }

    @Override
    public String getBrowserTimingFooter() {

        return "";
    }

    @Override
    public String getBrowserTimingFooter(String nonce) {

        return "";
    }

    @Override
    public void setUserName(String name) {

    }

    @Override
    public void setAccountName(String name) {

    }

    @Override
    public void setProductName(String name) {

    }

    @Override
    public void setAppServerPort(int port) {

    }

    @Override
    public void setServerInfo(String dispatcherName, String version) {

    }

    @Override
    public void setInstanceName(String instanceName) {

    }

    @Override
    public void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {

    }

}
