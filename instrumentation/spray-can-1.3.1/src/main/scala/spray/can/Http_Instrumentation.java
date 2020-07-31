/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import akka.actor.ActorRef;
import akka.io.Inet;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.api.agent.weaver.Weave;
import scala.Option;
import scala.collection.immutable.Traversable;
import spray.can.server.ServerSettings;
import spray.io.ServerSSLEngineProvider;

@Weave(originalName = "spray.can.Http")
public class Http_Instrumentation {

    @Weave(originalName = "spray.can.Http$Bind")
    public static class Bind {

        public Bind(final ActorRef listener, final InetSocketAddress endpoint, final int backlog,
                final Traversable<Inet.SocketOption> options, final Option<ServerSettings> settings,
                final ServerSSLEngineProvider sslEngineProvider) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Setting spray-can port to: {0,number,#}",
                    endpoint.getPort());
            AgentBridge.publicApi.setAppServerPort(endpoint.getPort());

            AgentBridge.publicApi.setServerInfo("spray-can HTTP", ManifestUtils.getVersionFromManifest(getClass(),
                    "spray-can", "1.3.1-1.3.3"));
        }

    }

}
