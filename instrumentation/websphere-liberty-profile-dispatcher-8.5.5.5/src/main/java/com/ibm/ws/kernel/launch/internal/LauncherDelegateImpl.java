/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.ibm.ws.kernel.launch.internal;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class LauncherDelegateImpl {
    private final BootstrapConfig config = Weaver.callOriginal();

    public void launchFramework() {
        Map<String, String> frameworkProperties = this.config.getFrameworkProperties();
        final String productInfoKey = "websphere.product.info";
        final String serverNameKey = "wlp.server.name";
        String productInfo = frameworkProperties.get(productInfoKey);
        String serverName = frameworkProperties.get(serverNameKey);

        if (productInfo != null) {
            // productInfo example: WebSphere Application Server 8.5.5.3 (wlp-1.0.6.cl50320140731-0257)
            Pattern dispatcherPattern = Pattern.compile("([\\w\\s]+)\\s([\\d\\.]+)\\s.+");
            Matcher matcher = dispatcherPattern.matcher(productInfo);
            if (matcher.find()) {
                String dispatcherName = matcher.group(1);
                if ("WebSphere Application Server".equals(dispatcherName)) {
                    dispatcherName += " Liberty profile";
                } // else dispatcher will be Open Liberty
                String version = matcher.group(2);
                AgentBridge.publicApi.setServerInfo(dispatcherName, version);
            }
        }

        if (serverName != null) {
            AgentBridge.publicApi.setInstanceName(serverName);
        }
        Weaver.callOriginal();
    }
}
