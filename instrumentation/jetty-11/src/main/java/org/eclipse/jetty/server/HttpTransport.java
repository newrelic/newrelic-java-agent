/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

@Weave(type = MatchType.Interface, originalName = "org.eclipse.jetty.server.HttpTransport")
public abstract class HttpTransport {

    // This method signature was changed in Jetty Server 10. Only weaving this to prevent this module from applying
    // to Jetty 9 and causing double instrumentation via the jetty-9.3 and jetty-10 instrumentation modules.
    public void send(MetaData.Request request, MetaData.Response response, ByteBuffer content, boolean lastContent, Callback callback) {
        Weaver.callOriginal();
    }

}
