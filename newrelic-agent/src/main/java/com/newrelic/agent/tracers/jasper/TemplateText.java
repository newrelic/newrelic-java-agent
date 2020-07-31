/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

// a proxy to org/apache/jasper/compiler/Node$TemplateText
public interface TemplateText {

    String getText() throws Exception;

    void setText(String text) throws Exception;
}
