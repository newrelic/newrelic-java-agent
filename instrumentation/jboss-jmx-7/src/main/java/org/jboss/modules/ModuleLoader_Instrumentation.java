/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.modules;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import java.util.logging.Level;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.api.agent.NewRelic;
import com.nr.agent.instrumentation.jboss.JBossUtils;

@Weave(type = MatchType.ExactClass, originalName = "org.jboss.modules.ModuleLoader")
public class ModuleLoader_Instrumentation {

    static void installMBeanServer() {

        final ScheduledExecutorService scheduler
                 = Executors.newSingleThreadScheduledExecutor();

        Runnable task = new Runnable() {
		public void run() {
			JBossUtils.addJmx();
			scheduler.shutdown();
            NewRelic.getAgent().getLogger().log(Level.FINER, "JBoss7 JMX monitoring service has been installed");
		}
        };

        int jmxServiceDelay = NewRelic.getAgent().getConfig().getValue("jboss7_jmxService_delay", 0);
        scheduler.schedule(task, jmxServiceDelay, TimeUnit.SECONDS);
        NewRelic.getAgent().getLogger().log(Level.FINER, "JBoss7 invoking JMX service with delay {0} sec", jmxServiceDelay);
        Weaver.callOriginal();
    }
}

