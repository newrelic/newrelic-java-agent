/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.management;

import javax.management.MBeanServer;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class AdminServiceImpl {

    // Instance variable on the admin service used to pull mbeans
    private MBeanServer _mbServer;

    // This method is called on WebSphere startup. Since this method is always called
    // we are using this method to grab the instance variable _mbServer.
    public String getJvmType() {

        AgentBridge.privateApi.addMBeanServer(_mbServer);

        return Weaver.callOriginal();
    }

}
