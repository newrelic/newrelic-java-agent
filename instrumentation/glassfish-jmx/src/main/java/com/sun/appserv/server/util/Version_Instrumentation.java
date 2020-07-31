/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.sun.appserv.server.util;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.glassfishjmx.GlassFishJmxUtils;

@Weave(type = MatchType.ExactClass, originalName = "com.sun.appserv.server.util.Version")
public class Version_Instrumentation {

    public static String getMajorVersion() {
        GlassFishJmxUtils.addJmx();
        return Weaver.callOriginal();
    }
}
