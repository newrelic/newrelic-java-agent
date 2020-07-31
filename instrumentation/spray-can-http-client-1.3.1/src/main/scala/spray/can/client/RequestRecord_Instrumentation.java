/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can.client;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import spray.http.HttpRequestPart;

@Weave(originalName = "spray.can.client.ClientFrontend$RequestRecord")
public class RequestRecord_Instrumentation {

    public HttpRequestPart request() {
        return Weaver.callOriginal();
    }

}
