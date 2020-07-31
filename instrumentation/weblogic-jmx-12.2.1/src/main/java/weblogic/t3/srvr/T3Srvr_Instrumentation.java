/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.t3.srvr;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

@Weave(originalName = "weblogic.t3.srvr.T3Srvr")
public class T3Srvr_Instrumentation {

    @NewField
    private static final String JMX_PREFIX = "com.bea";

    @NewField
    private static final AtomicBoolean addedJmx = new AtomicBoolean(false);

    public T3ServerFuture run(String[] args) {
        if (addedJmx.compareAndSet(false, true)) {
            AgentBridge.jmxApi.addJmxMBeanGroup(JMX_PREFIX);
            AgentBridge.getAgent().getLogger().log(Level.FINER, "Added JMX for Weblogic 12.2.1+");
        }
        return Weaver.callOriginal();
    }
}
