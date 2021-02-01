/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.modules;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jboss.JBossUtils;

@Weave(type = MatchType.ExactClass, originalName = "org.jboss.modules.ModuleLoader")
public class ModuleLoader_Instrumentation {

    public static void installMBeanServer() {
        JBossUtils.addJmx();
        Weaver.callOriginal();
    }
}
