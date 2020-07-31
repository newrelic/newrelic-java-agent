/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.AccessMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AllMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactReturnTypeMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.security.ProtectionDomain;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Executor;

public class OptimizedClassMatcherTest {
    private ClassMatchVisitorFactory matcher;
    private ClassAndMethodMatcher match1;
    private ClassAndMethodMatcher match2;
    private ClassAndMethodMatcher match3;
    private DefaultClassAndMethodMatcher match4;
    private DefaultClassAndMethodMatcher match5;
    private ClassAndMethodMatcher match6;
    private ClassAndMethodMatcher match7;

    private ClassAndMethodMatcher annotationMatch;

    @Before
    public void before() {
        initializeServiceManager();

        OptimizedClassMatcherBuilder builder = OptimizedClassMatcherBuilder.newBuilder();

        match1 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(Collection.class), new ExactMethodMatcher(
                "size", "()I"));

        match2 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(List.class), new ExactMethodMatcher("get",
                "(I)Ljava/lang/Object;"));
        match3 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(InvocationHandler.class),
                new ExactMethodMatcher("invoke",
                        "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;"));

        match4 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(Statement.class), new AllMethodsMatcher());

        match5 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(Runnable.class),
                OrMethodMatcher.getMethodMatcher(new NameMethodMatcher("run"), new ExactMethodMatcher("bogus", "()V")));

        match6 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(Map.class), new AccessMethodMatcher(
                Opcodes.ACC_PUBLIC));

        match7 = new DefaultClassAndMethodMatcher(createInterfaceMatcher(List.class), new ExactMethodMatcher("add",
                "(Ljava/lang/Object;)Z"));

        annotationMatch = new DefaultClassAndMethodMatcher(new AllClassesMatcher(), new AnnotationMethodMatcher(
                Type.getType(MyAnnotation.class)));

        // this is a bogus matcher to attempt to throw off match5
        builder.addClassMethodMatcher(new DefaultClassAndMethodMatcher(createInterfaceMatcher(PreparedStatement.class),
                new NameMethodMatcher("run")));

        builder.addClassMethodMatcher(match1);
        builder.addClassMethodMatcher(match2);
        builder.addClassMethodMatcher(match3);
        builder.addClassMethodMatcher(match4);
        builder.addClassMethodMatcher(match5);
        builder.addClassMethodMatcher(match6);
        builder.addClassMethodMatcher(match7);
        builder.addClassMethodMatcher(annotationMatch);
        this.matcher = builder.build();
    }

    private void initializeServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        Map<String, Object> confProps = new HashMap<>();
        confProps.put(AgentConfigImpl.APP_NAME, "Hello");
        MockConfigService service = new MockConfigService(AgentConfigImpl.createAgentConfig(confProps));
        serviceManager.setConfigService(service);
    }

    @Test
    public void testSlowMatch() throws IOException {

        Match match = getMatch(matcher, TestStatement.class);
        Assert.assertNotNull(match);
        Assert.assertEquals(3, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("close", "()V")));
        Assert.assertTrue(match.getMethods().contains(new Method("cancel", "()V")));
        Assert.assertTrue(match.getMethods().contains(new Method("executeBatch", "()[I")));

        Assert.assertEquals(1, match.getClassMatches().size());
        Assert.assertTrue(match.getClassMatches().containsKey(match4));
    }

    @Test
    public void testSingleMatch() throws IOException {

        Match match = getMatch(matcher, HashSet.class);
        Assert.assertNotNull(match);
        Assert.assertEquals(1, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("size", "()I")));

        Assert.assertEquals(1, match.getClassMatches().size());
        Assert.assertTrue(match.getClassMatches().containsKey(match1));
    }

    @Test
    public void testAccessMatch() throws IOException {

        Match match = getMatch(matcher, HashMap.class);
        Assert.assertNotNull(match);
        Assert.assertTrue(match.getMethods().size() > 0);
        Assert.assertTrue(match.getMethods().contains(new Method("size", "()I")));
        Assert.assertFalse(match.getMethods().contains(new Method("init", "()V")));

        Assert.assertTrue(match.getClassMatches().containsKey(match6));
    }

    @Test
    public void testSingleMatch2() throws IOException {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                return null;
            }
        };

        Match match = getMatch(matcher, handler.getClass());
        Assert.assertNotNull(match);
        Assert.assertEquals(1, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(
                new Method("invoke",
                        "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;")));

        Assert.assertEquals(1, match.getClassMatches().size());
        Assert.assertTrue(match.getClassMatches().containsKey(match3));
    }

    @Test
    public void testOrMatch() throws IOException {
        Runnable run = new Runnable() {
            public void run() {
            }
        };
        Match match = getMatch(matcher, run.getClass());
        Assert.assertNotNull(match);
        Assert.assertEquals(1, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("run", "()V")));

        Assert.assertEquals(1, match.getClassMatches().size());
        Assert.assertTrue(match.getClassMatches().containsKey(match5));
    }

    @Test
    public void testMultipleMatch() throws IOException {

        Match match = getMatch(matcher, ArrayList.class);
        Assert.assertNotNull(match);
        Assert.assertEquals(3, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("size", "()I")));
        Assert.assertTrue(match.getMethods().contains(new Method("get", "(I)Ljava/lang/Object;")));

        Assert.assertEquals(3, match.getClassMatches().size());
        Assert.assertTrue(match.getClassMatches().containsKey(match1));
        Assert.assertTrue(match.getClassMatches().containsKey(match2));
    }

    @Test
    public void testNonMatch() throws IOException {

        Match match = getMatch(matcher, Executor.class);
        Assert.assertNull(match);
    }

    @Test
    public void testGenericsMatch() throws IOException {

        InstrumentationContext instrumentationContext = getInstrumentationContext(matcher, ArgGenerics.class);
        Match match = instrumentationContext.getMatches().values().iterator().next();
        Assert.assertNotNull(match);
        Assert.assertEquals(3, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("add", "(Ljava/lang/Object;)Z")));

        Assert.assertEquals(match.getMethods().size(), match.getClassMatches().size());
        Assert.assertTrue(match.getClassMatches().containsKey(match7));

        Assert.assertEquals(2, instrumentationContext.getBridgeMethods().size());
        Assert.assertEquals(new Method("add", "(Ljava/lang/String;)Z"), instrumentationContext.getBridgeMethods().get(
                new Method("add", "(Ljava/lang/Object;)Z")));
    }

    @Test
    public void testAnnotationMatch() throws IOException {

        InstrumentationContext instrumentationContext = getInstrumentationContext(matcher, AnnotatedClass.class);
        Match match = instrumentationContext.getMatches().values().iterator().next();
        Assert.assertNotNull(match);
        Assert.assertEquals(2, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("annotation1", "()V")));
        Assert.assertTrue(match.getMethods().contains(new Method("annotation2", "()Z")));
    }

    @Test
    public void testReturnTypeMatch() throws IOException {
        ClassMatchVisitorFactory matcher = OptimizedClassMatcherBuilder.newBuilder().addClassMethodMatcher(
                new DefaultClassAndMethodMatcher(new AllClassesMatcher(), new ExactReturnTypeMethodMatcher(
                        Type.getType(List.class)))).build();

        InstrumentationContext instrumentationContext = getInstrumentationContext(matcher, Arrays.class);
        Assert.assertFalse(instrumentationContext.getMatches().isEmpty());
        Match match = instrumentationContext.getMatches().values().iterator().next();
        Assert.assertNotNull(match);
        Assert.assertEquals(1, match.getMethods().size());
        Assert.assertTrue(match.getMethods().contains(new Method("asList", "([Ljava/lang/Object;)Ljava/util/List;")));
        Assert.assertEquals(1, match.getClassMatches().size());
    }

    static final Method[] OBJECT_METHODS = new Method[] { new Method("equals", "(Ljava/lang/Object;)Z"),
            new Method("toString", "()Ljava/lang/String;"), new Method("finalize", "()V"),
            new Method("hashCode", "()I") };

    @Test
    public void testSkipObjectMethods_exactMatch() throws IOException {
        List<MethodMatcher> matchers = new ArrayList<>();
        for (Method m : OBJECT_METHODS) {
            matchers.add(new ExactMethodMatcher(m.getName(), m.getDescriptor()));
        }
        ClassMatchVisitorFactory matcher = OptimizedClassMatcherBuilder.newBuilder().addClassMethodMatcher(
                new DefaultClassAndMethodMatcher(new AllClassesMatcher(), OrMethodMatcher.getMethodMatcher(matchers))).build();

        Match match = getMatch(matcher, MyObject.class);
        Assert.assertNull(match);
    }

    @Test
    public void testSkipObjectMethods_looseMatch() throws IOException {
        ClassMatchVisitorFactory matcher = OptimizedClassMatcherBuilder.newBuilder().addClassMethodMatcher(
                new DefaultClassAndMethodMatcher(new AllClassesMatcher(), new AllMethodsMatcher())).build();

        Match match = getMatch(matcher, MyObject.class);
        Assert.assertNotNull(match);
        for (Method m : OBJECT_METHODS) {
            Assert.assertFalse(match.getMethods().contains(m));
        }
    }

    private Match getMatch(ClassMatchVisitorFactory matcher2, Class<?> clazz) throws IOException {
        InstrumentationContext context = getInstrumentationContext(matcher2, clazz);
        return context.getMatches().isEmpty() ? null : context.getMatches().values().iterator().next();
    }

    private InstrumentationContext getInstrumentationContext(ClassMatchVisitorFactory matcher2, Class<?> clazz)
            throws IOException {
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        ClassReader reader = ClassMatcherTest.getClassReader(clazz);
        InstrumentationContext context = new InstrumentationContext(null, null, Mockito.mock(ProtectionDomain.class));
        context.match(loader, null, reader, Arrays.asList(matcher2));
        return context;
    }

    private InterfaceMatcher createInterfaceMatcher(Class<?> interfaceClass) {
        return new InterfaceMatcher(Type.getType(interfaceClass).getInternalName());
    }

    private abstract static class TestStatement implements Statement {

        @Override
        public void close() throws SQLException {
        }

        @Override
        public void cancel() throws SQLException {
        }

        @Override
        public int[] executeBatch() throws SQLException {
            return null;
        }

    }

    private static class ArgGenerics implements List<String> {

        @Override
        public boolean add(String e) {
            System.err.println("Dude");
            return true;
        }

        @Override
        public int size() {

            return 0;
        }

        @Override
        public boolean isEmpty() {

            return false;
        }

        @Override
        public boolean contains(Object o) {

            return false;
        }

        @Override
        public Iterator<String> iterator() {

            return null;
        }

        @Override
        public Object[] toArray() {

            return null;
        }

        @Override
        public <T> T[] toArray(T[] a) {

            return null;
        }

        @Override
        public boolean remove(Object o) {

            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            for (String s : c) {
                add(s);
            }
            return false;
        }

        @Override
        public boolean addAll(int index, Collection<? extends String> c) {

            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {

            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {

            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public String get(int index) {

            return null;
        }

        @Override
        public String set(int index, String element) {

            return null;
        }

        @Override
        public void add(int index, String element) {

        }

        @Override
        public String remove(int index) {

            return null;
        }

        @Override
        public int indexOf(Object o) {

            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {

            return 0;
        }

        @Override
        public ListIterator<String> listIterator() {

            return null;
        }

        @Override
        public ListIterator<String> listIterator(int index) {

            return null;
        }

        @Override
        public List<String> subList(int fromIndex, int toIndex) {

            return null;
        }

    }

    private @interface MyAnnotation {

    }

    private static final class AnnotatedClass {

        public void noAnnotation() {

        }

        @MyAnnotation
        public void annotation1() {

        }

        @MyAnnotation
        public boolean annotation2() {
            return false;
        }
    }

    private static final class MyObject {

        @Override
        public int hashCode() {

            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {

            return super.equals(obj);
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {

            return super.clone();
        }

        @Override
        public String toString() {

            return super.toString();
        }

        @Override
        protected void finalize() throws Throwable {

            super.finalize();
        }

    }

}
