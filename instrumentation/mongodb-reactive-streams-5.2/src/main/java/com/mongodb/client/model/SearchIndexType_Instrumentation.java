/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.mongodb.client.model;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

// Weaving this class makes sure this instrumentation module does not apply to earlier versions of mongodb-reactive-streams
@Weave(type= MatchType.Interface, originalName = "com.mongodb.client.model.SearchIndexType")
public class SearchIndexType_Instrumentation {
}
