/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.google.common.collect.ImmutableMap;

import java.text.MessageFormat;
import java.util.Map;

public class HttpError extends Exception {

    private static final long serialVersionUID = 1L;

    // Immutable map representing all http response exception codes that the agent can receive from the collector
    private static final Map<Integer, String> RESPONSE_MESSAGES = ImmutableMap.<Integer, String>builder()
            .put(HttpResponseCode.BAD_REQUEST, "{0} received a bad request format ({1})") // 400
            .put(HttpResponseCode.UNAUTHORIZED, "An authentication error occurred ({1})") // 401 aka LicenseException
            .put(HttpResponseCode.FORBIDDEN, "An access error occurred ({1})") // 403
            .put(HttpResponseCode.NOT_FOUND, "Request not found ({1})") // 404
            .put(HttpResponseCode.METHOD_NOT_ALLOWED, "HTTP method not allowed ({1})") // 405
            .put(HttpResponseCode.PROXY_AUTHENTICATION_REQUIRED, "{} received a request lacking proper proxy authentication credentials ({1})") // 407
            .put(HttpResponseCode.REQUEST_TIMEOUT, "Request to {0} timed out ({1})") // 408
            .put(HttpResponseCode.CONFLICT, "Request could not be completed due to a conflict ({1})") // 409 aka ForceRestartException
            .put(HttpResponseCode.GONE, "Requested resource is no longer available ({1})") // 410 aka ForceDisconnectException
            .put(HttpResponseCode.LENGTH_REQUIRED, "{0} received a request with no Content-Length specified ({1})") // 411
            .put(HttpResponseCode.REQUEST_ENTITY_TOO_LARGE, "The data post was too large ({1})") // 413
            .put(HttpResponseCode.REQUEST_URI_TOO_LONG, "Request-URI is too long ({1})") // 414
            .put(HttpResponseCode.UNSUPPORTED_MEDIA_TYPE, "An error occurred serializing data ({1})") // 415
            .put(HttpResponseCode.EXPECTATION_FAILED, "Expectation set in Expect request-header by agent could not be met ({1})") // 417
            .put(HttpResponseCode.TOO_MANY_REQUESTS, "{0} has received too many requests ({1})") // 429
            .put(HttpResponseCode.REQUEST_HEADER_FIELDS_TOO_LARGE, "Request header fields are too large ({1})") // 431
            .put(HttpResponseCode.INTERNAL_SERVER_ERROR, "{0} encountered an internal error ({1})") // 500
            .put(HttpResponseCode.SERVICE_UNAVAILABLE, "{0} is temporarily unavailable ({1})") // 503
            .build();

    private final int statusCode;
    private final int entitySizeInBytes;

    public HttpError(String message, int statusCode, int entitySizeInBytes) {
        super(message == null ? Integer.toString(statusCode) : message);
        this.statusCode = statusCode;
        this.entitySizeInBytes = entitySizeInBytes;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getEntitySizeInBytes() {
        return entitySizeInBytes;
    }

    public static HttpError create(int statusCode, String host, int entitySizeInBytes) {
        String messageFormat = RESPONSE_MESSAGES.get(statusCode);
        if (messageFormat == null) {
            messageFormat = "Received a {1} response from {0}";
        }

        String message = MessageFormat.format(messageFormat, host, statusCode);

        return new HttpError(message, statusCode, entitySizeInBytes);
    }

    /**
     * A method to determine if the agent should retry an endpoint that returns an error response code.
     * As of Protocol 17 all agent endpoint error responses will be retried except for '410 GONE'
     *
     * @return true if request should be retried on the next harvest, else false
     */
    public boolean isRetryableError() {
        return statusCode != HttpResponseCode.GONE;
    }

    /**
     * A method to determine if the agent's payload was too large.
     *
     * @return true if a '413 REQUEST_ENTITY_TOO_LARGE' response is received, false for all other responses
     */
    public boolean isRequestPayloadTooLarge() {
        return statusCode == HttpResponseCode.REQUEST_ENTITY_TOO_LARGE;
    }

    /**
     * A method to determine if the agent should discard its harvest data when receiving an error response.
     * As of Protocol 17 all error responses except for 408, 429, 500, and 503 should cause the agent to discard its harvest data.
     *
     * @return true if the agent should discard its harvest data, false if the harvest data should be retained
     */
    public boolean discardHarvestData() {
        return statusCode != HttpResponseCode.REQUEST_TIMEOUT
                && statusCode != HttpResponseCode.TOO_MANY_REQUESTS
                && statusCode != HttpResponseCode.INTERNAL_SERVER_ERROR
                && statusCode != HttpResponseCode.SERVICE_UNAVAILABLE;
    }

}
