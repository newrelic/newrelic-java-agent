/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.api.agent.weaver.Weave;
import scala.Function0;
import scala.concurrent.Future;
import scala.runtime.BoxedUnit;

@Weave
public class Http {

    @Weave
    public static class ServerBinding {

        public ServerBinding(final InetSocketAddress endpoint, final Function0<Future<BoxedUnit>> unbindAction) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Setting akka-http port to: {0,number,#}",
                    endpoint.getPort());
            AgentBridge.publicApi.setAppServerPort(endpoint.getPort());

            AgentBridge.publicApi.setServerInfo("Akka HTTP", ManifestUtils.getVersionFromManifest(getClass(),
                    "akka-http-core", "1.0-2.0-M2"));
        }

    }
    
}
