/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

/**
 * When an instrumented method is invoked, this object is created to track the original class/method signature that was
 * instrumented. The class name of the signature might differ from the class name of the actual invoked object,
 * typically the case when the instrumented signature is an interface or a superclass.
 * 
 * This class is performance sentitive and its state is immutable. Don't modify this class unless you understand the
 * implications. If possible, we try to cache and reuse instances of this class.
 */
public final class ClassMethodSignature {

    private final String className;
    private final String methodName;
    private final String methodDesc;
    private ClassMethodMetricNameFormat customMetricName;
    private ClassMethodMetricNameFormat javaMetricName;

    /**
     * Creates a class method signature. The class name must be the dot separated name, ie java.lang.List, NOT the
     * internal name.
     * 
     * Performance note: method names and method descriptors are defined as constants in classes, so they can be
     * interned without using additional perm gen memory (there's still some overhead in the intern lookup). The
     * internal class name (the forward slash delimited name) is also a constant. I'm almost certain the '.' delimited
     * name is also interned. It's not a constant in the class file, but it may be that the class loaders intern it. As
     * I ran a test in which I changed our primary class transformer to convert every internal class name to the dot
     * delimited version and interned that string. It had no effect on the perm gen heap, which indicates the string is
     * already interned. Besides, we often inject the dot delimited name into classes, so it will appear in the consts
     * table.
     * 
     * @param className The dot separated class name, not the internal class name.
     * @param methodName
     * @param methodDesc
     */
    public ClassMethodSignature(String className, String methodName, String methodDesc) {
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;

        customMetricName = new ClassMethodMetricNameFormat(this, null, "Custom");
        javaMetricName = new ClassMethodMetricNameFormat(this, null, "Java");

    }

    /**
     * Returns the class name that was instrumented, which is not necessarily the class name of the object being
     * invoked. The servlet tracer, for example, will return GenericServlet for the class name of all instrumented
     * servlets.
     * 
     */
    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    @Override
    public String toString() {
        return new StringBuilder(className).append('.').append(methodName).append(methodDesc).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((methodDesc == null) ? 0 : methodDesc.hashCode());
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ClassMethodSignature other = (ClassMethodSignature) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (methodDesc == null) {
            if (other.methodDesc != null) {
                return false;
            }
        } else if (!methodDesc.equals(other.methodDesc)) {
            return false;
        }
        if (methodName == null) {
            if (other.methodName != null) {
                return false;
            }
        } else if (!methodName.equals(other.methodName)) {
            return false;
        }
        return true;
    }

    public ClassMethodSignature intern() {
        return new ClassMethodSignature(className.intern(), methodName.intern(), methodDesc.intern());
    }

    public MetricNameFormat getMetricNameFormat(String targetClassName, int flags) {

        if (targetClassName == null || className.equals(targetClassName)) {
            return TracerFlags.isCustom(flags) ? customMetricName : javaMetricName;
        } else {
            return new ClassMethodMetricNameFormat(this, targetClassName, TracerFlags.isCustom(flags) ? "Custom"
                    : "Java");
        }
    }

}
