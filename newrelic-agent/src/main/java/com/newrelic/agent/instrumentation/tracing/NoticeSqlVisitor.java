/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.google.common.collect.ImmutableMap;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * If the noticeSql() method signature changes this class will need to be updated. The test that checks for this is TraceMethodVisitorTest.
 */
public class NoticeSqlVisitor extends ClassVisitor {

    // The order of these arguments is important as they mirror the method arguments.
    // The key in this map is the name of the setter used by the SqlTracer interface.
    private static final Map<String, Type> setterNamesToTypes = ImmutableMap.of(
            "provideConnection", Type.getType(Connection.class),
            "setRawSql", Type.getType(String.class),
            "setParams", Type.getType(Object[].class)
            );

    private static final Method noticeSqlMethod = new Method("noticeSql", Type.VOID_TYPE,
            setterNamesToTypes.values().toArray(new Type[setterNamesToTypes.size()]));

    private final Set<Method> noticeSqlMethods;

    public NoticeSqlVisitor(int api) {
        super(api);
        this.noticeSqlMethods = new HashSet<>();
    }

    @Override
    public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc, String signature,
            String[] exceptions) {
        return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (isNoticeSqlMethod(owner, name, desc)) {
                    noticeSqlMethods.add(new Method(methodName, methodDesc));
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };
    }

    public Set<Method> getNoticeSqlMethods() {
        return noticeSqlMethods;
    }

    public static boolean isNoticeSqlMethod(String owner, String name, String desc) {
        return owner.equals(BridgeUtils.DATASTORE_METRICS_TYPE.getInternalName())
                && name.equals(noticeSqlMethod.getName())
                && desc.equals(noticeSqlMethod.getDescriptor());
    }

    public static int getSqlTracerSettersCount() {
        return setterNamesToTypes.size();
    }

    public static Iterator<Map.Entry<String, Type>> getSqlTracerSettersInReverseOrder() {
        LinkedList<Map.Entry<String, Type>> entries = new LinkedList<>(
                setterNamesToTypes.entrySet());

        return entries.descendingIterator();
    }
}
