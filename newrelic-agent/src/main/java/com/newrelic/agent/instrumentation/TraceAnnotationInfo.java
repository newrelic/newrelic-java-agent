/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

public class TraceAnnotationInfo {

    boolean dispatcher;
    String metricName;
    String tracerFactoryName;
    boolean skipTransactionTrace;

    public boolean dispatcher() {
        return dispatcher;
    }

    public String metricName() {
        return metricName;
    }

    public String tracerFactoryName() {
        return tracerFactoryName;
    }

    public boolean skipTransactionTrace() {
        return skipTransactionTrace;
    }

}
