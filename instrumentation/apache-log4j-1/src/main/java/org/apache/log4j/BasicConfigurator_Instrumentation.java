/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.log4j;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import static com.nr.agent.instrumentation.log4j1.Log4j1Util.setLog4j1Enabled;

@Weave(originalName = "org.apache.log4j.BasicConfigurator")
public class BasicConfigurator_Instrumentation {
    static public void configure() {
        setLog4j1Enabled();
        Weaver.callOriginal();
    }

    static public void configure(Appender appender) {
        setLog4j1Enabled();
        Weaver.callOriginal();
    }
}