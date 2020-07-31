/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.util.List;

public interface Normalizer {

    /**
     * Normalize the given name.
     * 
     * @return either null for ignore; the given name if not normalized; or the normalized name
     */
    String normalize(String name);

    /**
     * For testing.
     */
    List<NormalizationRule> getRules();

}