/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.kafka;

interface CachedKafkaMetric {
    boolean isValid();

    String displayName();

    void report(final FiniteMetricRecorder recorder);
}
