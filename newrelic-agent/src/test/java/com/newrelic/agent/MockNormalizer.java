/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.normalization.NormalizationRule;
import com.newrelic.agent.normalization.Normalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockNormalizer implements Normalizer {

    private Map<String, String> normalizationResults = new HashMap<>();

    public void setNormalizationResults(Map<String, String> normalizationResults) {
        this.normalizationResults = normalizationResults;
    }

    @Override
    public String normalize(String name) {
        if (normalizationResults.containsKey(name)) {
            return normalizationResults.get(name);
        }
        return name;
    }

    @Override
    public List<NormalizationRule> getRules() {
        return null;
    }

}
