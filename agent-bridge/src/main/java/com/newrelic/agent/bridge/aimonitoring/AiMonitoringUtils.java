/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.aimonitoring;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;

public class AiMonitoringUtils {
    // Enabled defaults
    private static final boolean AI_MONITORING_ENABLED_DEFAULT = false;
    private static final boolean AI_MONITORING_STREAMING_ENABLED_DEFAULT = true;
    private static final boolean AI_MONITORING_RECORD_CONTENT_ENABLED_DEFAULT = true;
    private static final boolean HIGH_SECURITY_ENABLED_DEFAULT = false;
    // collect_ai is an account wide server side config sent in the connect response to disable AIM
    public static final String COLLECT_AI = "collect_ai";

    /**
     * Check if ai_monitoring features are enabled.
     * Indicates whether LLM instrumentation will be registered. If this is set to False, no metrics, events, or spans are to be sent.
     *
     * @return true if AI monitoring is enabled, else false
     */
    public static boolean isAiMonitoringEnabled() {
        Config config = NewRelic.getAgent().getConfig();
        Boolean collectAi = config.getValue("collect_ai");
        Boolean aimEnabled = config.getValue("ai_monitoring.enabled", AI_MONITORING_ENABLED_DEFAULT);
        Boolean highSecurity = config.getValue("high_security", HIGH_SECURITY_ENABLED_DEFAULT);

        if (collectAi != null && !collectAi) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "AIM: AI Monitoring is disabled due to account wide server side config.");
            NewRelic.incrementCounter("Supportability/Java/ML/Disabled");
            return false;
        } else if (highSecurity || !aimEnabled) {
            aimEnabled = false;
            String disabledReason = highSecurity ? "High Security Mode." : "agent config.";
            NewRelic.getAgent().getLogger().log(Level.FINE, "AIM: AI Monitoring is disabled due to " + disabledReason);
            NewRelic.incrementCounter("Supportability/Java/ML/Disabled");
        } else {
            NewRelic.incrementCounter("Supportability/Java/ML/Enabled");
        }

        return aimEnabled;
    }

    /**
     * Check if ai_monitoring.streaming features are enabled.
     *
     * @return true if streaming is enabled, else false
     */
    public static boolean isAiMonitoringStreamingEnabled() {
        Boolean enabled = NewRelic.getAgent().getConfig().getValue("ai_monitoring.streaming.enabled", AI_MONITORING_STREAMING_ENABLED_DEFAULT);

        if (enabled) {
            NewRelic.incrementCounter("Supportability/Java/ML/Streaming/Enabled");
        } else {
            NewRelic.incrementCounter("Supportability/Java/ML/Streaming/Disabled");
        }

        return enabled;
    }

    /**
     * Check if the input and output content should be added to LLM events.
     *
     * @return true if adding content is enabled, else false
     */
    public static boolean isAiMonitoringRecordContentEnabled() {
        Boolean enabled = NewRelic.getAgent().getConfig().getValue("ai_monitoring.record_content.enabled", AI_MONITORING_RECORD_CONTENT_ENABLED_DEFAULT);

        if (enabled) {
            NewRelic.incrementCounter("Supportability/Java/ML/RecordContent/Enabled");
        } else {
            NewRelic.incrementCounter("Supportability/Java/ML/RecordContent/Disabled");
        }

        return enabled;
    }
}
