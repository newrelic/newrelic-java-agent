/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.aimonitoring;

import com.newrelic.api.agent.NewRelic;

public class AiMonitoringUtils {
    // Enabled defaults
    private static final boolean AI_MONITORING_ENABLED_DEFAULT = false;
    private static final boolean AI_MONITORING_STREAMING_ENABLED_DEFAULT = true;
    private static final boolean AI_MONITORING_RECORD_CONTENT_ENABLED_DEFAULT = true;

    /**
     * Check if ai_monitoring features are enabled.
     * Indicates whether LLM instrumentation will be registered. If this is set to False, no metrics, events, or spans are to be sent.
     *
     * @return true if AI monitoring is enabled, else false
     */
    public static boolean isAiMonitoringEnabled() {
        return NewRelic.getAgent().getConfig().getValue("ai_monitoring.enabled", AI_MONITORING_ENABLED_DEFAULT);
    }

    /**
     * Check if ai_monitoring.streaming features are enabled.
     *
     * @return true if streaming is enabled, else false
     */
    public static boolean isAiMonitoringStreamingEnabled() {
        return NewRelic.getAgent().getConfig().getValue("ai_monitoring.streaming.enabled", AI_MONITORING_STREAMING_ENABLED_DEFAULT);
    }

    /**
     * Check if the input and output content should be added to LLM events.
     *
     * @return true if adding content is enabled, else false
     */
    public static boolean isAiMonitoringRecordContentEnabled() {
        return NewRelic.getAgent().getConfig().getValue("ai_monitoring.record_content.enabled", AI_MONITORING_RECORD_CONTENT_ENABLED_DEFAULT);
    }
}
