/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina.core;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.tomcat.TomcatUtils;
import org.apache.catalina.LifecycleException;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.catalina.core.StandardServer")
public class StandardServer_Instrumentation {

    protected void startInternal() throws LifecycleException {
        TomcatUtils.addJmx();
        Weaver.callOriginal();
    }
}
