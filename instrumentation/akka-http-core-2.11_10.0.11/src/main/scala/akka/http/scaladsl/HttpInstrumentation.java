/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function0;
import scala.Function1;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.net.InetSocketAddress;
import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "akka.http.scaladsl.Http")
public class HttpInstrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "akka.http.scaladsl.Http$ServerBinding")
    public static class ServerBinding {

        public InetSocketAddress localAddress() {
            return Weaver.callOriginal();
        }

        @WeaveAllConstructors
        public ServerBinding() {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Setting akka-http port to: {0,number,#}", localAddress().getPort());
            AgentBridge.publicApi.setAppServerPort(localAddress().getPort());
            AgentBridge.publicApi.setServerInfo("Akka HTTP", ManifestUtils.getVersionFromManifest(getClass(), "akka-http-core", "10.0.11"));

            AgentBridge.instrumentation.retransformUninstrumentedClass(SyncRequestHandler.class);
            AgentBridge.instrumentation.retransformUninstrumentedClass(AsyncRequestHandler.class);
        }
    }

}
