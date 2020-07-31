/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;
import com.newrelic.agent.MetricNames;

public class NormalizerFactory {

    public static Normalizer createUrlNormalizer(String appName, List<NormalizationRule> urlRules) {
        return new UrlNormalizer(new NormalizerImpl(appName, urlRules));
    }

    public static Normalizer createTransactionNormalizer(String appName, List<NormalizationRule> transactionNameRules,
            List<TransactionSegmentTerms> transactionSegmentTermRules) {

        // ordering is important here. We want to first apply the old transaction name rules
        Normalizer normalizer = new NormalizerImpl(appName, transactionNameRules);

        // then apply the segment term rules if there are any
        if (!transactionSegmentTermRules.isEmpty()) {
            normalizer = compoundNormalizer(normalizer, createTransactionSegmentNormalizer(transactionSegmentTermRules));
        }

        return normalizer;
    }

    private static Normalizer compoundNormalizer(final Normalizer... normalizers) {
        final List<NormalizationRule> rules = new ArrayList<>();
        for (Normalizer n : normalizers) {
            rules.addAll(n.getRules());
        }
        return new Normalizer() {

            @Override
            public String normalize(String name) {
                for (Normalizer n : normalizers) {
                    name = n.normalize(name);
                    // stop and return null whenever a null value is returned
                    if (name == null) {
                        return name;
                    }
                }
                return name;
            }

            @Override
            public List<NormalizationRule> getRules() {
                return rules;
            }
        };
    }

    static Normalizer createTransactionSegmentNormalizer(final List<TransactionSegmentTerms> transactionSegmentTermRules) {

        return new Normalizer() {

            @Override
            public String normalize(String name) {

                for (TransactionSegmentTerms terms : transactionSegmentTermRules) {
                    if (name.startsWith(terms.prefix)) {
                        String afterPrefix = name.substring(terms.prefix.length() + 1);
                        String[] segments = afterPrefix.split(MetricNames.SEGMENT_DELIMITER_STRING);
                        List<String> keep = new ArrayList<>(segments.length + 1);

                        keep.add(terms.prefix);

                        boolean discarded = false;
                        for (String segment : segments) {
                            if (terms.terms.contains(segment)) {
                                keep.add(segment);
                                discarded = false;
                            } else if (!discarded) {
                                keep.add("*");
                                discarded = true;
                            }
                        }

                        name = Joiner.on(MetricNames.SEGMENT_DELIMITER).join(keep);
                    }
                }

                return name;
            }

            @Override
            public List<NormalizationRule> getRules() {
                return Collections.emptyList();
            }
        };
    }

    public static Normalizer createMetricNormalizer(String appName, List<NormalizationRule> metricNameRules) {
        return new NormalizerImpl(appName, metricNameRules);
    }

    private static class UrlNormalizer implements Normalizer {

        private final Normalizer normalizer;

        private UrlNormalizer(Normalizer normalizer) {
            this.normalizer = normalizer;
        }

        @Override
        public String normalize(String name) {
            // name can be null in some instances
            if (name == null) {
                return null;
            }
            String normalizedName = name;
            if (!normalizedName.startsWith(MetricNames.SEGMENT_DELIMITER_STRING)) {
                normalizedName = MetricNames.SEGMENT_DELIMITER_STRING + name;
            }
            return normalizer.normalize(normalizedName);
        }

        @Override
        public List<NormalizationRule> getRules() {
            return normalizer.getRules();
        }

    }

}
