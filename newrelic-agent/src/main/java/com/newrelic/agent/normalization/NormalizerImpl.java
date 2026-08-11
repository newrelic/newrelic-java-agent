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
import java.util.concurrent.CopyOnWriteArrayList;
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
        this.rules = new CopyOnWriteArrayList<>(rules);  // Thread safe mutable list
    }

    @Override
    public String normalize(String name) {
        // some of the rules perform pattern matching which can throw null pointers
        if (name == null) {
            return null;
        }

        String normalizedName = name;
        for (NormalizationRule rule : rules) {
            // This call can throw exceptions, specifically if the replacement String uses a grouping
            // index value for a non-existent group in the normalization rule pattern
            RuleResult result;
            try {
                result = rule.normalize(normalizedName);
                if (!result.isMatch()) {
                    continue;
                }
            } catch (Exception e) {
                // Log the exception and remove this rule from the rule list so it doesn't execute again
                String msg = MessageFormat.format("Rule \"{0}\" has thrown an exception during normalization. " +
                                "It will be removed for this agent run but it should be corrected/deleted prior " +
                                "to the next restart. Exception: {1}", rule, e.getMessage());
                Agent.LOG.warning(msg);
                rules.remove(rule);
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
        return Collections.unmodifiableList(rules);
    }

}
