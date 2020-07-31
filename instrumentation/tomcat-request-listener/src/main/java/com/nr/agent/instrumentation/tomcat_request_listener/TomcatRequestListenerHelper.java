/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.tomcat_request_listener;

public final class TomcatRequestListenerHelper {

    public static final ThreadLocal<Boolean> requestDestroyedNeeded = new ThreadLocal<Boolean>() { };

}