/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

/**
 * Encapsulates an HTTP Response. Note that {@link DataSenderImpl} only cares about a single header, so it's expected
 * that the {@link HttpClientWrapper} implementation pre-extracts the header value.
 */
public class ReadResult {
    private final int statusCode;
    private final String responseBody;
    private final String proxyAuthenticateHeader;

    ReadResult(int statusCode, String responseBody, String proxyAuthenticateHeader) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.proxyAuthenticateHeader = proxyAuthenticateHeader;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getResponseBody() {
        return responseBody;
    }

    String getProxyAuthenticateHeader() {
        return proxyAuthenticateHeader;
    }

    public static ReadResult create(int statusCode, String responseBody, String proxyAuthenticateHeader) {
        return new ReadResult(statusCode, responseBody, proxyAuthenticateHeader);
    }

}
