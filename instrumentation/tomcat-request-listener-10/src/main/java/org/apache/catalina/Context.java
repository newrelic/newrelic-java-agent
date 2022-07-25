/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.catalina;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import jakarta.servlet.ServletRequest;

import static com.nr.agent.instrumentation.tomcat_request_listener.TomcatRequestListenerHelper.requestDestroyedNeeded;

@Weave(type = MatchType.Interface)
public abstract class Context {

    public boolean fireRequestInitEvent(ServletRequest request) {
        try {
            return Weaver.callOriginal();
        } finally {
            // We saw a requestInit event, mark this thread as possibly needing a manual requestDestroy
            requestDestroyedNeeded.set(true);
        }
    }

    public boolean fireRequestDestroyEvent(ServletRequest request) {
        try {
            return Weaver.callOriginal();
        } finally {
            // We saw a requestDestroy event, no need to manually destroy this transaction
            requestDestroyedNeeded.set(false);
        }
    }

}
