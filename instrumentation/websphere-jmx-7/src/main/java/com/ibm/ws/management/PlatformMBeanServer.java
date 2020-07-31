/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.management;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class PlatformMBeanServer implements javax.management.MBeanServer {

    // We have security problems with the MBeanServer when
    // global security is enabled - we add a different MBeanServer
    // in AdminServiceImpl
    public void setAdminProps(java.util.Properties props) {
        AgentBridge.privateApi.removeMBeanServer(this);
        Weaver.callOriginal();
    }

}
