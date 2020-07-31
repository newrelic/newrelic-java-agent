/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package weblogic.servlet.internal;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class ErrorManager {

    public void handleException(ServletRequestImpl request, ServletResponseImpl response, Throwable t) {
        NewRelic.noticeError(t);
        Weaver.callOriginal();
    }
}
