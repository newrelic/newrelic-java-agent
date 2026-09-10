/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.sling.api;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

/**
 * This weave class is present so this module only applies when the Apache Sling
 * api classes are present. Otherwise we would end up instrumenting anything that
 * extended javax.servlet.ServletResponse, which would be very bad.
 */
@Weave(originalName = "org.apache.sling.api.SlingConstants", type = MatchType.ExactClass)
public class Require_SlingConstants {
}
