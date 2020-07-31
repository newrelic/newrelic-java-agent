/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.caucho.server.resin;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.resin.ResinUtils;

@Weave(type = MatchType.ExactClass, originalName = "com.caucho.server.resin.Resin")
public class Resin_Instrumentation {

    public void start() {
        ResinUtils.addJmx();
        Weaver.callOriginal();
    }
}
