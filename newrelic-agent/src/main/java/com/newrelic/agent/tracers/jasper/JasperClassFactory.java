/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JasperClassFactory {

    static final Map<ClassLoader, JasperClassFactory> classFactories = new HashMap<>();

    private final Method visitTemplateTextMethod;
    private final Method templateTextGetTextMethod;
    private final Method templateTextSetTextMethod;
    private final Class<?> scriptletClass;
    private final Class<?> nodeClass;
    private final Class<?> markClass;
    private final Method generateVisitorVisitScriptletMethod;
    private Method visitorVisitScriptletMethod;
    private Method nodeGetParentMethod;
    private Method nodeGetQName;

    private JasperClassFactory(ClassLoader classloader) throws Exception {
        Class<?> generateVisitorClass = classloader.loadClass("org.apache.jasper.compiler.Generator$GenerateVisitor");
        Class<?> templateTextClass = classloader.loadClass("org.apache.jasper.compiler.Node$TemplateText");

        visitTemplateTextMethod = generateVisitorClass.getMethod("visit", templateTextClass);
        visitTemplateTextMethod.setAccessible(true);

        templateTextGetTextMethod = templateTextClass.getMethod("getText");
        templateTextGetTextMethod.setAccessible(true);

        templateTextSetTextMethod = templateTextClass.getMethod("setText", String.class);
        templateTextSetTextMethod.setAccessible(true);

        scriptletClass = classloader.loadClass("org.apache.jasper.compiler.Node$Scriptlet");
        generateVisitorVisitScriptletMethod = generateVisitorClass.getMethod("visit", scriptletClass);
        generateVisitorVisitScriptletMethod.setAccessible(true);

        Class<?> visitorClass = classloader.loadClass("org.apache.jasper.compiler.Node$Visitor");
        visitorVisitScriptletMethod = visitorClass.getMethod("visit", scriptletClass);
        visitorVisitScriptletMethod.setAccessible(true);

        markClass = classloader.loadClass("org.apache.jasper.compiler.Mark");
        nodeClass = classloader.loadClass("org.apache.jasper.compiler.Node");

        nodeGetParentMethod = nodeClass.getMethod("getParent");
        nodeGetParentMethod.setAccessible(true);
        nodeGetQName = nodeClass.getMethod("getQName");
        nodeGetQName.setAccessible(true);
    }

    public Object createScriptlet(String script) throws Exception {
        Constructor<?> constructor = scriptletClass.getDeclaredConstructor(String.class, markClass, nodeClass);
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance(script, null, null);
    }

    public GenerateVisitor getGenerateVisitor(Object visitor) {
        return new GenerateVisitorImpl(visitor);
    }

    public Node getNode(Object node) {
        return new NodeImpl(node);
    }

    public Visitor getVisitor(Object visitor) {
        return new VisitorImpl(visitor);
    }

    public TemplateText getTemplateText(Object text) {
        return new TemplateTextImpl(text);
    }

    public static synchronized JasperClassFactory getJasperClassFactory(ClassLoader cl) throws Exception {
        JasperClassFactory factory = classFactories.get(cl);
        if (factory == null) {
            factory = new JasperClassFactory(cl);
            classFactories.put(cl, factory);
        }
        return factory;
    }

    private class NodeImpl implements Node {
        private final Object node;

        public NodeImpl(Object node) {
            super();
            this.node = node;
        }

        @Override
        public Node getParent() throws Exception {
            return new NodeImpl(nodeGetParentMethod.invoke(node));
        }

        @Override
        public String getQName() throws Exception {
            return (String) nodeGetQName.invoke(node);
        }

    }

    private class VisitorImpl implements Visitor {

        private Object visitor;

        public VisitorImpl(Object visitor) {
            this.visitor = visitor;
        }

        @Override
        public void writeScriptlet(String script) throws Exception {
            visitorVisitScriptletMethod.invoke(visitor, createScriptlet(script));
        }

    }

    private class GenerateVisitorImpl implements GenerateVisitor {

        private final Object visitor;

        public GenerateVisitorImpl(Object visitor) {
            this.visitor = visitor;
        }

        @Override
        public void visit(TemplateText text) throws Exception {
            visitTemplateTextMethod.invoke(visitor, ((TemplateTextImpl) text).text);
        }

        @Override
        public void writeScriptlet(String script) throws Exception {
            generateVisitorVisitScriptletMethod.invoke(visitor, createScriptlet(script));
        }
    }

    private class TemplateTextImpl implements TemplateText {
        final Object text;

        public TemplateTextImpl(Object text) {
            super();
            this.text = text;
        }

        @Override
        public String getText() throws Exception {
            return (String) templateTextGetTextMethod.invoke(text);
        }

        @Override
        public void setText(String text) throws Exception {
            templateTextSetTextMethod.invoke(this.text, text);
        }

    }
}
