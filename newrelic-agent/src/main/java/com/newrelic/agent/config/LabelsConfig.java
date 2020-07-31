/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

/**
 * Config section for labels.
 */
public interface LabelsConfig {

    /**
     * Labels, with name as the entry's key and value as value.
     *
     * @return labels
     */
    Map<String, String> getLabels();

}
