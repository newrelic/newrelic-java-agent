/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.violation;

import org.objectweb.asm.commons.Method;

import com.google.common.base.MoreObjects;

/**
 * A violation of the weave API. Used to validate @Weave classes against original classes.
 */
public class WeaveViolation {
    private final WeaveViolationType type;
    private String clazz;
    private String field;
    private Method method;

    /**
     * Create a class-level violation with the specified type and class.
     * @param type violation type
     * @param clazz weave class name
     */
    public WeaveViolation(WeaveViolationType type, String clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    /**
     * Create a field-level violation with the specified type, class, and field.
     * @param type violation type
     * @param clazz weave class name
     * @param field weave field name
     */
    public WeaveViolation(WeaveViolationType type, String clazz, String field) {
        this(type, clazz);
        this.field = field;
    }

    /**
     * Create a method-level violation with the specified type, class, and field.
     * @param type violation type
     * @param clazz weave class name
     * @param method weave method
     */
    public WeaveViolation(WeaveViolationType type, String clazz, Method method) {
        this(type, clazz);
        this.method = method;
    }

    /**
     * The type of violation.
     * @return type of violation
     */
    public WeaveViolationType getType() {
        return type;
    }

    /**
     * The weave class name that caused the violation.
     * @return weave class name
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * The weave field name that caused the violation, or <code>null</code> if not applicable.
     * @return weave field name, or <code>null</code> if not applicable
     */
    public String getField() {
        return field;
    }

    /**
     * The weave method that caused the violation, or <code>null</code> if not applicable.
     * @return weave method, or <code>null</code> if not applicable
     */
    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("type", type).add("clazz", clazz).add("field",
                field).add("method", method).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WeaveViolation violation = (WeaveViolation) o;

        if (type != violation.type) {
            return false;
        }
        if (clazz != null ? !clazz.equals(violation.clazz) : violation.clazz != null) {
            return false;
        }
        if (field != null ? !field.equals(violation.field) : violation.field != null) {
            return false;
        }
        return !(method != null ? !method.equals(violation.method) : violation.method != null);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
        result = 31 * result + (field != null ? field.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }
}
