/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.log4j.spi;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.Hashtable;

@Weave(originalName = "org.apache.log4j.spi.LoggingEvent")
public class LoggingEvent_Instrumentation {
    private Hashtable mdcCopy = Weaver.callOriginal();
}
