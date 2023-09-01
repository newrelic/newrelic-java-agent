/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.logging.log4j.core.layout;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.Map;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

abstract class AbstractJacksonLayout_Instrumentation {

    @Weave(originalName = "org.apache.logging.log4j.core.layout.AbstractJacksonLayout$LogEventWithAdditionalFields", type = MatchType.ExactClass)
    public static class LogEventWithAdditionalFields_Instrumentation {

        private final Object logEvent;
        private final Map<String, String> additionalFields;

        public LogEventWithAdditionalFields_Instrumentation(final Object logEvent, final Map<String, String> additionalFields) {
            // This will only ever work if at least one additional field is set in the JsonLayout, doesn't matter what it is
            //  <JsonLayout complete="false" compact="false">
            //	    <KeyValuePair key="ADD-NR-LINKING-METADATA" value="true" />
            //	</JsonLayout>

            this.logEvent = Weaver.callOriginal();
            if (isApplicationLoggingEnabled()) {
                if (isApplicationLoggingLocalDecoratingEnabled()) {
                    // Append New Relic linking metadata from agent to log message
                    additionalFields.put("In NR", getLinkingMetadataBlob());
                }
            }
            this.additionalFields = Weaver.callOriginal();
        }
    }

}
