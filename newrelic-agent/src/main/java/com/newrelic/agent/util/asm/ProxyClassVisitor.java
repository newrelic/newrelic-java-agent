/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import java.lang.reflect.Method;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.newrelic.weave.utils.WeaveUtils;

public class ProxyClassVisitor extends ClassVisitor {

    private static final String PROXY_METHOD_DESC = Type.getDescriptor(Method.class);

    private boolean hasProxyMethod = false;

    public ProxyClassVisitor() {
        super(WeaveUtils.ASM_API_LEVEL);
    }

    public ProxyClassVisitor(ClassVisitor cv) {
        super(WeaveUtils.ASM_API_LEVEL, cv);
    }

    /**
     * Returns true if the input class is a proxy class. A proxy class is defined to be a class that is final, extends
     * java.lang.reflect.Proxy, and has a java.lang.reflect.Method field. The check that the class is final and extends
     * java.lang.reflect.Proxy should be called prior to this ProxyClassVisitor.
     * 
     * @return True if the class is a proxy, else false.
     */
    public boolean isProxy() {
        return (hasProxyMethod);
    }

    /**
     * Visits a field of the class. Determines if there is a field with a java.lang.reflect.Method.
     * 
     * @param access the field's access flags (see {@link Opcodes}). This parameter also indicates if the field is
     *        synthetic and/or deprecated.
     * @param name the field's name.
     * @param desc the field's descriptor (see {@link Type Type}).
     * @param signature the field's signature. May be <tt>null</tt> if the field's type does not use generic types.
     * @param value the field's initial value. This parameter, which may be <tt>null</tt> if the field does not have an
     *        initial value, must be an {@link Integer}, a {@link Float}, a {@link Long}, a {@link Double} or a
     *        {@link String} (for <tt>int</tt>, <tt>float</tt>, <tt>long</tt> or <tt>String</tt> fields respectively).
     *        <i>This parameter is only used for static fields</i>. Its value is ignored for non static fields, which
     *        must be initialized through bytecode instructions in constructors or methods.
     * @return a visitor to visit field annotations and attributes, or <tt>null</tt> if this class visitor is not
     *         interested in visiting these annotations and attributes.
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!hasProxyMethod && (desc.equals(PROXY_METHOD_DESC))) {
            hasProxyMethod = true;
        }
        return super.visitField(access, name, desc, signature, value);
    }

}
