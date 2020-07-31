
/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.startup;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.tomcat.TomcatUtils;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.catalina.startup.HostConfig")
public class HostConfig_Instrumentation {

    public void start() {
        TomcatUtils.addJmx();
        Weaver.callOriginal();
    }
}