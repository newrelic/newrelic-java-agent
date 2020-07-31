/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.metricname;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.util.Strings;

/**
 * Modifying this class may affect performance because we create a lot of instances of this class. Be careful when
 * making changes.
 */
public class ClassMethodMetricNameFormat extends AbstractMetricNameFormat {

    private String metricName;
    private final ClassMethodSignature signature;
    private final String className;
    private final String prefix;

    public ClassMethodMetricNameFormat(ClassMethodSignature sig, Object object) {
        this(sig, object, "Java");
    }

    public ClassMethodMetricNameFormat(ClassMethodSignature sig, Object object, String prefix) {
        this(sig, object == null ? sig.getClassName() : object.getClass().getName(), prefix);
    }

    public ClassMethodMetricNameFormat(ClassMethodSignature sig, String objectClassName) {
        this(sig, objectClassName, "Java");
    }

    public ClassMethodMetricNameFormat(ClassMethodSignature sig, String objectClassName, String prefix) {
        this.signature = sig;
        this.className = objectClassName == null ? sig.getClassName() : objectClassName;
        this.prefix = prefix;
    }

    @Override
    public String getMetricName() {
        // we delay building the metricName because we create and throw away many instances of this class without
        // calling getMetricName()
        if (metricName == null) {
            metricName = Strings.join('/', prefix, className, signature.getMethodName());
        }
        return metricName;
    }

    public static String getMetricName(ClassMethodSignature sig, Object object) {
        return getMetricName(sig, object, "Java");
    }

    public static String getMetricName(ClassMethodSignature sig, Object object, String prefix) {
        if (object == null) {
            return getMetricName(sig, prefix);
        }
        return Strings.join('/', prefix, object.getClass().getName(), sig.getMethodName());
    }

    public static String getMetricName(ClassMethodSignature sig, String prefix) {
        String className = sig.getClassName().replaceAll("/", ".");
        return Strings.join('/', prefix, className, sig.getMethodName());
    }
}
