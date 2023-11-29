/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.ee9.nested;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

@Weave(type = MatchType.ExactClass, originalName = "org.eclipse.jetty.ee9.nested.HttpChannel")
public abstract class HttpChannel_Instrumentation {

//    protected boolean sendResponse(MetaData.Response response, ByteBuffer content, boolean complete, final Callback callback) {
//        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
//        return Weaver.callOriginal();
//    }
//
//    public boolean sendResponse(MetaData.Response info, ByteBuffer content, boolean complete) {
//        NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
//        return Weaver.callOriginal();
//    }

}
