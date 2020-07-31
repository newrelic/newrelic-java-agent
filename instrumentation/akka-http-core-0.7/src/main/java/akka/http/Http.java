/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import akka.http.server.ServerSettings;
import akka.io.Inet;
import akka.stream.MaterializerSettings;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.api.agent.weaver.Weave;
import scala.Option;
import scala.collection.immutable.Traversable;

@Weave
public class Http {

    @Weave
    public static class Bind {

        public Bind(final InetSocketAddress endpoint, final int backlog,
                final Traversable<Inet.SocketOption> options, final Option<ServerSettings> serverSettings,
                final Option<MaterializerSettings> materializerSettings) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Setting akka-http port to: {0,number,#}",
                    endpoint.getPort());
            AgentBridge.publicApi.setAppServerPort(endpoint.getPort());

            AgentBridge.publicApi.setServerInfo("Akka HTTP", ManifestUtils.getVersionFromManifest(getClass(),
                    "akka-http-core", "0.7-0.11"));
        }

    }
    
}
