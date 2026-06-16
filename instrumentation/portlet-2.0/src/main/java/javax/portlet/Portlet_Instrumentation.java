/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package javax.portlet;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "javax.portlet.Portlet", type = MatchType.Interface)
public class Portlet_Instrumentation {
    @Trace
    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) {
        Weaver.callOriginal();
    }

    @Trace
    public void render(RenderRequest renderRequest, RenderResponse renderResponse) {
        Weaver.callOriginal();
    }
}
