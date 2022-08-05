/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey.client;

import java.net.URI;

public class JerseyClientUtils {

    public static final String JERSEY_CLIENT = "Jersey-Client";
    public static final String JERSEY_SEGMENT_NAME = "Async-Jersey-Client";
    public static final String FAILED = "failed";
    public static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

}
