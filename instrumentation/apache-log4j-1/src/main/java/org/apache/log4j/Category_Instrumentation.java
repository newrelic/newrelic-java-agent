/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.log4j;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.log4j.spi.LoggingEvent;

import static com.nr.agent.instrumentation.log4j1.Log4j1Util.generateMetricsAndOrLogEventIfEnabled;

@Weave(originalName = "org.apache.log4j.Category")
public class Category_Instrumentation {

    public void callAppenders(LoggingEvent event) {
        generateMetricsAndOrLogEventIfEnabled(event);
        Weaver.callOriginal();
    }
}
