/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

/**
 * Constants representing the HTTP response codes that an agent can receive from an agent endpoint.
 */
public class HttpResponseCode {
    // successful 200 or 202 responses, preserve agent run
    public static final int OK = 200;
    public static final int ACCEPTED = 202;

    // Behavior: discard harvest data, preserve agent run, retry request on next harvest
    public static final int BAD_REQUEST = 400;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int PROXY_AUTHENTICATION_REQUIRED = 407;
    public static final int LENGTH_REQUIRED = 411;
    public static final int REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int REQUEST_URI_TOO_LONG = 414;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int EXPECTATION_FAILED = 417;
    public static final int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

    // Behavior: retain harvest data, preserve agent run, retry request on next harvest
    public static final int REQUEST_TIMEOUT = 408;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    // Behavior: discard harvest data, restart agent, retry request on next harvest
    public static final int UNAUTHORIZED = 401;
    public static final int CONFLICT = 409;

    // Behavior: discard harvest data, shutdown agent, do not retry request
    public static final int GONE = 410;
}
