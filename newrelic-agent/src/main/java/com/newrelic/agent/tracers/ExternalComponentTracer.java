/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class ExternalComponentTracer extends AbstractExternalComponentTracer {

    public ExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host,
            String library, String uri, String... operations) {
        this(transaction, sig, object, host, library, false, uri, operations);
    }

    public ExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host,
            String library, boolean includeOperationInMetric, String uri, String... operations) {
        super(transaction, sig, object, host, library, includeOperationInMetric, uri, operations);
    }

    public ExternalComponentTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host,
            MetricNameFormat metricNameFormat) {
        super(transaction, sig, object, host, metricNameFormat);
    }

    @Override
    public void finish(Throwable throwable) {
        super.finish(throwable);
    }

    @Override
    public void finish(int opcode, Object returnValue) {
        super.finish(opcode, returnValue);
    }

}
