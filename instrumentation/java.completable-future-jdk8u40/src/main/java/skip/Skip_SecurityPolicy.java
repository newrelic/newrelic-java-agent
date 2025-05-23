/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package skip;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 *  * This class instructs JREs 11 and up to skip this instrumentation module,
 *  * and use java.completable-future-jdk11 instead.
 *  * java.net.http.WebSocket was chosen because it was introduced in Java 11.
 */
@SkipIfPresent(originalName = "java.net.http.WebSocket")
public class Skip_SecurityPolicy {
}
