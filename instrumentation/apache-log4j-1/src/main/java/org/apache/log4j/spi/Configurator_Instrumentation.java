/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.log4j.spi;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.io.InputStream;
import java.net.URL;

import static com.nr.agent.instrumentation.log4j1.Log4j1Util.setLog4j1Enabled;

@Weave(originalName = "org.apache.log4j.spi.Configurator", type = MatchType.Interface)
public class Configurator_Instrumentation {

    public void doConfigure(InputStream inputStream, LoggerRepository loggerRepository) {
        setLog4j1Enabled();
        Weaver.callOriginal();
    }

    public void doConfigure(URL url, LoggerRepository loggerRepository) {
        setLog4j1Enabled();
        Weaver.callOriginal();
    }

}
