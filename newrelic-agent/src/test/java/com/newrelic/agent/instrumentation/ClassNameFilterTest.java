/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.logging.IAgentLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

public class ClassNameFilterTest {
    private ClassNameFilter filter;

    @Before
    public void setup() throws Exception {

        filter = new ClassNameFilter(Mockito.mock(IAgentLogger.class));
        filter.addExclude("java/.*");
    }

    @Test
    public void test() throws Exception {
        filter.addExclude("^(java/|sun/|com/sun/|com/newrelic)(.*)");
        filter.addExclude("^(org/objectweb/asm/|javax/xml/|org/apache/juli/)(.*)");
        filter.addExclude("^org/apache/tomcat/dbcp/dbcp/Delegating(Statement|PreparedStatement|CallableStatement|Connection|ResultSet)$");
        filter.addExclude("com/dude/(.*)");

        Assert.assertTrue(filter.isExcluded("com/dude/Test"));
        Assert.assertFalse(filter.isExcluded("com/dude2/Test"));

        Assert.assertTrue(filter.isExcluded("sun/test/SomeClass"));
        Assert.assertTrue(filter.isExcluded("com/sun/test/SomeClass"));

        Assert.assertTrue(filter.isExcluded("org/objectweb/asm/ClassVisitor"));

        Assert.assertTrue(filter.isExcluded("java/util/Properties"));
        Assert.assertTrue(filter.isExcluded("org/apache/tomcat/dbcp/dbcp/DelegatingStatement"));
        Assert.assertTrue(filter.isExcluded("org/apache/tomcat/dbcp/dbcp/DelegatingPreparedStatement"));

        Assert.assertFalse(filter.isExcluded("org/apache/tomcat/dbcp/dbcp/DelegatingStatementDude"));

        Assert.assertTrue(filter.isExcluded("com/newrelic/agent/Agent"));
    }

    @Test
    public void test2() throws Exception {
        filter.addExclude("^org/apache/catalina/startup/Bootstrap");

        Assert.assertTrue(filter.isExcluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    @Test
    public void testGuiceProxyExclude() throws Exception {
        filter.addExclude(".*\\$\\$EnhancerByGuice\\$\\$.*");

        Assert.assertTrue(filter.isExcluded("com.addepar.server.api.contact.ContactResource$$EnhancerByGuice$$7a84356a."));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    @Test
    public void test3() throws Exception {
        filter.addExclude("org/apache/catalina/startup/Bootstrap");

        Assert.assertTrue(filter.isExcluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    @Test
    public void testExcludeInnerClass() throws Exception {
        filter.addExclude("test/ExcludeTest$ExcludeTestInner");

        Assert.assertTrue(filter.isExcluded("test/ExcludeTest$ExcludeTestInner"));
        Assert.assertFalse(filter.isIncluded("test/ExcludeTest$ExcludeTestInner"));
    }

    @Test
    public void testExcludeClass() throws Exception {
        filter.addExclude("org/apache/catalina/startup/Bootstrap");

        Assert.assertTrue(filter.isExcluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
        Assert.assertFalse(filter.isIncluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isIncluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    @Test
    public void testExcludeRegex() throws Exception {
        filter.addExclude("^org/apache/catalina/startup/Bootstrap$");

        Assert.assertTrue(filter.isExcluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
        Assert.assertFalse(filter.isIncluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isIncluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    @Test
    public void testExcludeRegex2() throws Exception {
        filter.addExclude(".*ByCGLIB\\$\\$.*");
        Assert.assertTrue(filter.isExcluded("org/apache/geronimo/tomcat/TomcatContext$$EnhancerByCGLIB$$d2039dcb$$FastClassByCGLIB$$76b56e40"));
        Assert.assertTrue(filter.isExcluded("org/apache/geronimo/kernel/KernelGBean$$FastClassByCGLIB$$1cccefc9"));
        Assert.assertTrue(filter.isExcluded("org/apache/geronimo/tomcat/TomcatContext$$EnhancerByCGLIB$$d2039dcb$$FastClassByCGLIB$$76b56e40"));
    }

    @Test
    public void testExcludeHikariProxy() throws Exception {
        filter.addExclude("^com/zaxxer/hikari/pool/.*Proxy.*");
        filter.addExclude("^com/zaxxer/hikari/proxy.*");
        Assert.assertTrue(filter.isExcluded("com/zaxxer/hikari/pool/HikariProxyStatement"));
        Assert.assertTrue(filter.isExcluded("com/zaxxer/hikari/pool/ProxyStatement"));
    }

    @Test
    public void testIncludeClass() throws Exception {
        filter.addInclude("org/apache/catalina/startup/Bootstrap");

        Assert.assertTrue(filter.isIncluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isIncluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
        Assert.assertFalse(filter.isExcluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    @Test
    public void testIncludeRegex() throws Exception {
        filter.addInclude("^org/apache/catalina/startup/Bootstrap$");

        Assert.assertTrue(filter.isIncluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isIncluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
        Assert.assertFalse(filter.isExcluded("org/apache/catalina/startup/Bootstrap"));
        Assert.assertFalse(filter.isExcluded("com/ibm/ws/webcontainer/servlet/ServletWrapper"));
    }

    // @Test
    public void testIncludePerformance() {
        List<String> classNames = new LinkedList<>();
        classNames.add("org/apache/catalina/startup/Bootstrap");
        classNames.add("org/apache/catalina/startup/Bootstrap2");
        classNames.add("org/apache/catalina/startup/Bootstrap3");
        classNames.add("org/apache/catalina/startup/Bootstrap4");
        classNames.add("org/apache/catalina/startup/Bootstrap5");
        classNames.add("org/apache/catalina/startup/Bootstrap6");
        classNames.add("org/apache/catalina/startup/Bootstrap7");
        classNames.add("org/apache/catalina/startup/Bootstrap8");
        classNames.add("org/apache/catalina/startup/Bootstrap9");
        classNames.add("org/apache/catalina/startup/Bootstrap10");

        for (String className : classNames) {
            filter.addIncludeClass(className);
        }

        classNames.add("org/apache/catalina/startup/Bootstrap11");
        classNames.add("org/apache/catalina/startup/Bootstrap12");
        classNames.add("org/apache/catalina/startup/Bootstrap13");
        classNames.add("org/apache/catalina/startup/Bootstrap14");
        classNames.add("org/apache/catalina/startup/Bootstrap15");
        classNames.add("org/apache/catalina/startup/Bootstrap16");
        classNames.add("org/apache/catalina/startup/Bootstrap17");
        classNames.add("org/apache/catalina/startup/Bootstrap18");
        classNames.add("org/apache/catalina/startup/Bootstrap19");
        classNames.add("org/apache/catalina/startup/Bootstrap20");

        int count = 100000;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            for (String className : classNames) {
                filter.isIncluded(className);
            }
        }
        String msg = MessageFormat.format("Time to check {0} classes = {1}", classNames.size() * count,
                System.currentTimeMillis() - startTime);
        System.out.println(msg);
    }

    /*
     * @Test public void httpConnection() { Assert.assertTrue(excludeClass(HttpURLConnection.class));
     * Assert.assertTrue(includeClass(HttpURLConnection.class)); }
     * 
     * @Test public void runtime() { Assert.assertTrue(excludeClass(Runtime.class));
     * Assert.assertTrue(includeClass(Runtime.class)); }
     * 
     * @Test public void process() { Assert.assertFalse(filter.includeClass("java/lang/Process"));
     * Assert.assertTrue(filter.includeClass("java/lang/UNIXProcess"));
     * Assert.assertTrue(filter.includeClass("java/lang/ProcessImpl")); }
     * 
     * @Test public void runnableAdapter() {
     * Assert.assertTrue(filter.excludeClass("java/util/concurrent/Executors$RunnableAdapter"));
     * Assert.assertTrue(filter.includeClass("java/util/concurrent/Executors$RunnableAdapter")); }
     */

    @Test
    public void string() {
        Assert.assertTrue(excludeClass(String.class));
        Assert.assertFalse(includeClass(String.class));
    }

    private boolean excludeClass(Class<?> clazz) {
        return filter.isExcluded(getName(clazz));
    }

    private boolean includeClass(Class<?> clazz) {
        return filter.isIncluded(getName(clazz));
    }

    private String getName(Class<?> clazz) {
        return clazz.getName().replace('.', '/');
    }
}
