/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can.client;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.collection.immutable.Queue;

@Weave(originalName = "spray.can.client.ClientFrontend$$anon$1$$anon$2")
public class ClientFrontendOuterPipeline_Instrumentation {

    public Queue<RequestRecord_Instrumentation> spray$can$client$ClientFrontend$$anon$$anon$$openRequests() {
        return Weaver.callOriginal();
    }

}
