/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Add an interface to the visited class or interface.
 */
public class AddInterfaceAdapter extends ClassVisitor {

    private final String className;
    private final Class<?> type;

    /**
     * 
     * @param type The interface to add to the visited class.
     * @param className The name of the visited class
     */
    public AddInterfaceAdapter(ClassVisitor cv, String className, Class<?> type) {
        super(WeaveUtils.ASM_API_LEVEL, cv);
        this.className = className;
        this.type = type;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, addInterface(interfaces));
    }

    @Override
    public void visitEnd() {
        if (Agent.LOG.isFinerEnabled()) {
            String msg = MessageFormat.format("Appended {0} to {1}", type.getName(), className.replace('/', '.'));
            Agent.LOG.finer(msg);
        }
        super.visitEnd();
    }

    private String[] addInterface(String[] interfaces) {
        Set<String> list = new HashSet<>(Arrays.asList(interfaces));
        list.add(Type.getType(type).getInternalName());
        return list.toArray(new String[list.size()]);
    }

}
