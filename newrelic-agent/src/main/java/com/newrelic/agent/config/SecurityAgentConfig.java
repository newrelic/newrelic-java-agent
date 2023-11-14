/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.Sets;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;

import java.util.Collections;
import java.util.Set;

/* Default config should look like:
 *
 * security:
 *   enabled: false
 *   low-priority-instrumentation:
 *     enabled: false
 *   mode: IAST
 *   validator_service_url: wss://csec.nr-data.net
 *   agent:
 *     enabled: false
 *   detection:
 *     rci:
 *       enabled: true
 *     rxss:
 *       enabled: true
 *     deserialization:
 *       enabled: true
 */
public class SecurityAgentConfig {
    public static final String SECURITY_AGENT_ENABLED = "security.agent.enabled";
    public static final boolean SECURITY_AGENT_ENABLED_DEFAULT = false;
    public static final String SECURITY_ENABLED = "security.enabled";
    public static final boolean SECURITY_ENABLED_DEFAULT = false;
    public static final String SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED = "security.low-priority-instrumentation.enabled";
    public static final boolean SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED_DEFAULT = false;
    public static final String SECURITY_MODE = "security.mode";
    public static final String SECURITY_MODE_DEFAULT = "IAST";
    public static final String SECURITY_VALIDATOR_SERVICE_URL = "security.validator_service_url";
    public static final String SECURITY_VALIDATOR_SERVICE_URL_DEFAULT = "wss://csec.nr-data.net";
    public static final String SECURITY_DETECTION_RCI_ENABLED = "security.detection.rci.enabled";
    public static final boolean SECURITY_DETECTION_RCI_ENABLED_DEFAULT = true;
    public static final String SECURITY_DETECTION_RXSS_ENABLED = "security.detection.rxss.enabled";
    public static final boolean SECURITY_DETECTION_RXSS_ENABLED_DEFAULT = true;
    public static final String SECURITY_DETECTION_DESERIALIZATION_ENABLED = "security.detection.deserialization.enabled";
    public static final boolean SECURITY_DETECTION_DESERIALIZATION_ENABLED_DEFAULT = true;
    private static final Config config = NewRelic.getAgent().getConfig();
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    public static final Set<String> SECURITY_AGENT_CLASS_TRANSFORMER_EXCLUDES_TO_IGNORE = Collections.unmodifiableSet(
            Sets.newHashSet("^java/security/.*", "^javax/crypto/.*", "^net/sf/saxon.*"));

    /**
     * Create supportability metrics showing the enabled status of the security agent.
     */
    public static void addSecurityAgentConfigSupportabilityMetrics() {
        String enabled = isSecurityEnabled() ? ENABLED : DISABLED;
        NewRelic.incrementCounter("Supportability/Java/SecurityAgent/Enabled/" + enabled);
        String agentEnabled = isSecurityAgentEnabled() ? ENABLED : DISABLED;
        NewRelic.incrementCounter("Supportability/Java/SecurityAgent/Agent/Enabled/" + agentEnabled);
    }

    /**
     * Determines whether the security agent should be initialized.
     *
     * @return True if security agent should be initialized, false if not
     */
    public static boolean shouldInitializeSecurityAgent() {
        return !config.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY) &&
                config.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT) &&
                (config.getValue(SECURITY_ENABLED) != null);
    }

    /**
     * Determines whether the security agent will be enabled or completely disabled.
     *
     * @return True if security agent should be enabled, false if it should be completely disabled
     */
    public static boolean isSecurityAgentEnabled() {
        return config.getValue(SECURITY_AGENT_ENABLED, SECURITY_AGENT_ENABLED_DEFAULT) &&
                !config.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY);
    }

    /**
     * Determines whether the security agent, once initialized, is allowed to send security data to New Relic.
     *
     * @return True if security agent should send data, false if it should not
     */
    public static boolean isSecurityEnabled() {
        return config.getValue(SECURITY_ENABLED, SECURITY_ENABLED_DEFAULT) &&
                !config.getValue(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY);
    }

    /**
     * Determines whether the security agent should detect RCI events.
     *
     * @return True if security agent should detect RCI events, false if it should not
     */
    public static boolean isSecurityDetectionRciEnabled() {
        return config.getValue(SECURITY_DETECTION_RCI_ENABLED, SECURITY_DETECTION_RCI_ENABLED_DEFAULT);
    }

    /**
     * Determines whether the security agent should detect RXSS events.
     *
     * @return True if security agent should detect RXSS events, false if it should not
     */
    public static boolean isSecurityDetectionRxssEnabled() {
        return config.getValue(SECURITY_DETECTION_RXSS_ENABLED, SECURITY_DETECTION_RXSS_ENABLED_DEFAULT);
    }

    /**
     * Determines whether the security agent should detect deserialization events.
     *
     * @return True if security agent should detect deserialization events, false if it should not
     */
    public static boolean isSecurityDetectionDeserializationEnabled() {
        return config.getValue(SECURITY_DETECTION_DESERIALIZATION_ENABLED, SECURITY_DETECTION_DESERIALIZATION_ENABLED_DEFAULT);
    }

    /**
     * Get the validator service URL that the security agent communicates with.
     *
     * @return String representing the validator service URL that the security agent communicates with
     */
    public static String getSecurityAgentValidatorServiceUrl() {
        return config.getValue(SECURITY_VALIDATOR_SERVICE_URL, SECURITY_VALIDATOR_SERVICE_URL_DEFAULT);
    }

    /**
     * Get the Security agent mode. Default is IAST.
     *
     * @return String representing the Security agent mode
     */
    public static String getSecurityAgentMode() {
        return config.getValue(SECURITY_MODE, SECURITY_MODE_DEFAULT);
    }

    /**
     * Determines whether the security agent low priority attack/vulnerability modules will instrument or not.
     *
     * @return True if security agent should instrument low priority attack/vulnerability modules, false if it should not
     */
    public static boolean isSecurityLowPriorityInstrumentationEnabled() {
        return config.getValue(SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED, SECURITY_LOW_PRIORITY_INSTRUMENTATION_ENABLED_DEFAULT);
    }

}
