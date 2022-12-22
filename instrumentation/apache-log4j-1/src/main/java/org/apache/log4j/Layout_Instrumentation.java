/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.log4j;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.log4j.spi.LoggingEvent;

import static com.nr.agent.instrumentation.log4j1.Log4j1Util.appendAgentMetadataIfLocalDecoratingEnabled;

@Weave(originalName = "org.apache.log4j.Layout", type = MatchType.BaseClass)
public class Layout_Instrumentation {
    public String format(LoggingEvent event) {
        String formattedMessage = Weaver.callOriginal();
        return appendAgentMetadataIfLocalDecoratingEnabled(formattedMessage);
    }
}