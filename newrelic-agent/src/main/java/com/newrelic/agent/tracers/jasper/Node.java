/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

// a proxy to org/apache/jasper/compiler/Node
public interface Node {

    Node getParent() throws Exception;

    String getQName() throws Exception;
}
