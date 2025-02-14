/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for application logging features. These settings do not pertain to agent logs.
 */
public interface ApplicationLoggingConfig {

    /**
     * Determines whether the application_logging features are completely disabled or can be controlled individually.
     *
     * @return true if the application_logging features can be controlled individually, false if the entire stanza is disabled
     */
    boolean isEnabled();

    /**
     * Allow metrics to be generated to provide data such as the number of lines logged at each log level.
     *
     * @return true is log metrics are enabled, otherwise false
     */
    boolean isMetricsEnabled();

    /**
     * Allow the agent to forward application logs to New Relic.
     *
     * @return true is log forwarding is enabled, otherwise false
     */
    boolean isForwardingEnabled();

    /**
     * Allow the agent to forward context data along with the application logs to New Relic.
     *
     * @return true if context data forwarding is enabled, false otherwise
     */
    boolean isForwardingContextDataEnabled();

    /**
     * Allow the agent to decorate application log files and console output with New Relic specific linking metadata.
     *
     * @return true is local log decorating is enabled, otherwise false
     */
    boolean isLocalDecoratingEnabled();

    /**
     * Get the max number of LogEvents that can be stored during a harvest period before sampling takes place.
     *
     * @return max number of LogEvents stored per harvest
     */
    int getMaxSamplesStored();

    /**
     * Get the include list for context data.
     *
     * @return
     */
    List<String> getForwardingContextDataInclude();

    /**
     * Get the exclude list for context data.
     *
     * @return a list of Strings
     */
    List<String> getForwardingContextDataExclude();

    /**
     * Allow the agent to add labels to application logs.
     *
     * @return true if logging labels are enabled, false otherwise
     */
    boolean isLogLabelsEnabled();

    /**
     * Removes excluded labels from the labels map.
     *
     * @return a map of filtered log labels
     */

    Map<String, String> removeExcludedLogLabels(Map<String, String> labels);

    /**
     * Get the set of excluded labels.
     *
     * @return a Set of excluded labels
     */
    Set<String> getLogLabelsExcludeSet();
}
