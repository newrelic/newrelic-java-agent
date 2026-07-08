/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.core.io.support;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.Utils;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Weave(type = MatchType.Interface, originalName = "org.springframework.core.io.support.ResourcePatternResolver")
public class ResourcePatternResolver_Instrumentation {

    public Resource[] getResources(String locationPattern) throws IOException {
        return Utils.excludeNRShadowedDependencies(Weaver.callOriginal());
    }
}
