/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

/**
 * A class for applying renaming rules.
 * 
 * This class is thread-safe.
 */
public class NormalizerImpl implements Normalizer {

    private final List<NormalizationRule> rules;
    private final String appName;

    public NormalizerImpl(String appName, List<NormalizationRule> rules) {
        this.appName = appName;
        this.rules = Collections.unmodifiableList(rules);
    }

    @Override
    public String normalize(String name) {
        // some of the rules perform pattern matching which can throw null pointers
        if (name == null) {
            return null;
        }

        String normalizedName = name;
        for (NormalizationRule rule : rules) {
            RuleResult result = rule.normalize(normalizedName);
            if (!result.isMatch()) {
                continue;
            }
            if (rule.isIgnore()) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Ignoring \"{0}\" for \"{1}\" because it matched rule \"{2}\"",
                            name, appName, rule);
                    Agent.LOG.finer(msg);
                }
                return null;
            }
            String replacement = result.getReplacement();
            if (replacement != null) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Normalized \"{0}\" to \"{2}\" for \"{1}\" using rule \"{3}\"",
                            name, appName, replacement, rule);
                    Agent.LOG.finer(msg);
                }
                normalizedName = replacement;
            }
            if (rule.isTerminateChain()) {
                break;
            }
        }
        return normalizedName;
    }

    @Override
    public List<NormalizationRule> getRules() {
        return rules;
    }

}
