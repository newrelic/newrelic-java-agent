/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.netty.handler.codec.http;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Prevent netty-3.4 from loading with 3.8
 */
@SkipIfPresent
public class DefaultHttpHeaders {
}