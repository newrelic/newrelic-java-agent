/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.api;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.weave.utils.SynchronizedMethodNode;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * This holds default implementations of our public api interfaces. When we add a new method to these interfaces we use
 * these default implementations to fill in the missing methods on older api implementations.
 */
public class DefaultApiImplementations {

    private final Map<String, Map<Method, MethodNode>> interfaceToMethods;

    public DefaultApiImplementations() throws Exception {
        this(DefaultRequest.class, DefaultResponse.class);
    }

    public DefaultApiImplementations(Class<?>... defaultImplementations) throws Exception {
        Map<String, Map<Method, MethodNode>> interfaceToMethods = new HashMap<>();
        for (Class<?> clazz : defaultImplementations) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new Exception(clazz.getName() + " cannot be abstract");
            }
            final ClassReader reader = Utils.readClass(clazz);
            String[] interfaces = reader.getInterfaces();
            if (interfaces.length != 1) {
                throw new Exception(clazz.getName() + " implements multiple interfaces: " + Arrays.asList(interfaces));
            }
            final Map<Method, MethodNode> methods = new HashMap<>();
            interfaceToMethods.put(interfaces[0], methods);

            ClassVisitor cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                        String[] exceptions) {
                    Method method = new Method(name, desc);
                    if ((access & Opcodes.ACC_FINAL) != 0) {
                        Agent.LOG.severe("Default implementation " + reader.getClassName() + " should not declared "
                                + method + " final");
                        return null;
                    }
                    MethodNode node = new SynchronizedMethodNode(access, name, desc, signature, exceptions);
                    methods.put(method, node);
                    return node;
                }

            };
            reader.accept(cv, ClassReader.SKIP_DEBUG);
            methods.remove(new Method("<init>", "()V"));
            methods.remove(new Method("<cinit>", "()V"));
        }
        this.interfaceToMethods = ImmutableMap.copyOf(interfaceToMethods);
    }

    public Map<String, Map<Method, MethodNode>> getApiClassNameToDefaultMethods() {
        return interfaceToMethods;
    }

    private static final class DefaultRequest implements Request {

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public Enumeration<?> getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }
    }

    private static final class DefaultResponse implements Response {

        @Override
        public int getStatus() throws Exception {
            return 0;
        }

        @Override
        public String getStatusMessage() throws Exception {
            return null;
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }
}
