/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package skip;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 *  * This Weave class instructs JREs 11 and up to skip this instrumentation module,
 *  * and use java.completable-future-jdk11 instead.
 *  * javax.security.auth.Policy was chosen because it was removed in Java 11.
 */
@SkipIfPresent(originalName = "javax.security.auth.Policy")
public class Skip_SecurityPolicy {
    //This does nothing
}
