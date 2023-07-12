/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.method;

import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.profile.MethodLineNumberMatcher;
import com.newrelic.agent.profile.TestFileWithLineNumbers;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Test;
import test.newrelic.test.agent.RpcCall;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MethodInfoWithLineNumbersTest {

    @Test
    public void testCreateMethodInfoForConstructorsWithLineNumbers() {
        List<String> args = verifyExactAndGetArgsForConstructors(new TestFileWithLineNumbersFunctional());
        Assert.assertEquals(0, args.size());

        args = verifyExactAndGetArgsForConstructors(new TestFileWithLineNumbersFunctional(1, 2L));
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("int", args.get(0));
        Assert.assertEquals("long", args.get(1));

        args = verifyExactAndGetArgsForConstructors(new TestFileWithLineNumbersFunctional(new int[0], new byte[0][0],
                new String[0][0][0]));
        Assert.assertEquals(3, args.size());
        Assert.assertEquals("int[]", args.get(0));
        Assert.assertEquals("byte[][]", args.get(1));
        Assert.assertEquals("java.lang.String[][][]", args.get(2));

        args = verifyExactAndGetArgsForConstructors(new TestFileWithLineNumbersFunctional(new Object(),
                new ArrayList<String>(), "1234", new HashSet<>()));
        Assert.assertEquals(4, args.size());
        Assert.assertEquals("java.lang.Object", args.get(0));
        Assert.assertEquals("java.util.List", args.get(1));
        Assert.assertEquals("java.lang.String", args.get(2));
        Assert.assertEquals("java.util.Set", args.get(3));
    }

    private List<String> verifyExactAndGetArgsForConstructors(TestFileWithLineNumbersFunctional newClass) {

        StackTraceElement trace = getTraceElementForConstructor(newClass.getTrace());
        MethodInfo info = MethodInfoUtil.createMethodInfo(TestFileWithLineNumbersFunctional.class, trace.getMethodName(),
                trace.getLineNumber());
        return verifyBasics(info);
    }

    private static StackTraceElement getTraceElementForConstructor(StackTraceElement[] elements) {
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("<init>")) {
                return current;
            }
        }
        return null;
    }

    @Test
    public void testCreateMethodInfoForMethods() {
        TestFileWithLineNumbers test = new TestFileWithLineNumbers();

        List<String> args = verifyExactAndGetArgsForMethods(test.foo());
        Assert.assertNotNull(args);
        Assert.assertEquals(0, args.size());

        args = verifyExactAndGetArgsForMethods(test.foo(Integer.valueOf(1), Integer.valueOf(2)));
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("java.lang.Integer", args.get(0));
        Assert.assertEquals("java.lang.Integer", args.get(1));

        args = verifyExactAndGetArgsForMethods(test.foo("test"));
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));

        args = verifyExactAndGetArgsForMethods(test.foo(1L, 2L));
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("long", args.get(0));
        Assert.assertEquals("long", args.get(1));

        args = verifyExactAndGetArgsForMethods(test.foo(1, 5));
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("int", args.get(0));
        Assert.assertEquals("int", args.get(1));

        args = verifyExactAndGetArgsForMethods(test.foo(Short.parseShort("1"), Byte.parseByte("0"), new int[0],
                new ArrayList<String>(), new HashMap<String, String>()));
        Assert.assertEquals(5, args.size());
        Assert.assertEquals("short", args.get(0));
        Assert.assertEquals("byte", args.get(1));
        Assert.assertEquals("int[]", args.get(2));
        Assert.assertEquals("java.util.List", args.get(3));
        Assert.assertEquals("java.util.Map", args.get(4));

        args = verifyExactAndGetArgsForMethods(test.foo(new Integer[0][0], new Object[0]));
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("java.lang.Integer[][]", args.get(0));
        Assert.assertEquals("java.lang.Object[]", args.get(1));
    }

    private List<String> verifyExactAndGetArgsForMethods(StackTraceElement[] fromClass) {

        StackTraceElement trace = getTraceElementForFoo(fromClass);
        MethodInfo info = MethodInfoUtil.createMethodInfo(TestFileWithLineNumbers.class, trace.getMethodName(),
                trace.getLineNumber());
        return verifyBasics(info);
    }

    private static List<String> verifyBasics(MethodInfo info) {
        Assert.assertNotNull(info);
        Assert.assertTrue(info instanceof ExactMethodInfo);
        List<Map<String, Object>> actual = info.getJsonMethodMaps();
        Assert.assertNotNull(actual);
        Assert.assertEquals(1, actual.size());
        Map<String, Object> methodData = actual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(1, methodData.size());
        List<String> args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        return args;
    }

    private static StackTraceElement getTraceElementForFoo(StackTraceElement[] elements) {
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("foo")) {
                return current;
            }
        }
        return null;
    }

    @Test
    public void testLineNumberConstructors() {
        int lineNumber = getLineNumberConstructor(new TestFileWithLineNumbers());
        String desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "<init>", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("()V", desc);

        lineNumber = getLineNumberConstructor(new TestFileWithLineNumbers(1, 2L));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "<init>", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("(IJ)V", desc);

        lineNumber = getLineNumberConstructor(new TestFileWithLineNumbers(new int[0], new byte[0][0],
                new String[0][0][0]));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "<init>", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("([I[[B[[[Ljava/lang/String;)V", desc);

        lineNumber = getLineNumberConstructor(new TestFileWithLineNumbers(new Object(), new ArrayList<String>(),
                "1234", new HashSet<>()));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "<init>", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("(Ljava/lang/Object;Ljava/util/List;Ljava/lang/String;Ljava/util/Set;)V", desc);
    }

    @Test
    public void testLineNumberMethods() {
        TestFileWithLineNumbers test = new TestFileWithLineNumbers();
        int lineNumber = getLineNumberFoo(test.foo());
        String desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("()[Ljava/lang/StackTraceElement;", desc);

        lineNumber = getLineNumberFoo(test.foo(Integer.valueOf(1), Integer.valueOf(2)));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("(Ljava/lang/Integer;Ljava/lang/Integer;)[Ljava/lang/StackTraceElement;", desc);

        lineNumber = getLineNumberFoo(test.foo("test"));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null. Line Number: " + lineNumber, desc);
        Assert.assertEquals("(Ljava/lang/String;)[Ljava/lang/StackTraceElement;", desc);

        lineNumber = getLineNumberFoo(test.foo(1L, 2L));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null", desc);
        Assert.assertEquals("(JJ)[Ljava/lang/StackTraceElement;", desc);

        lineNumber = getLineNumberFoo(test.foo("1L", "2L"));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null", desc);
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/StackTraceElement;", desc);

        lineNumber = getLineNumberFoo(test.foo(1, 5));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null", desc);
        Assert.assertEquals("(II)[Ljava/lang/StackTraceElement;", desc);

        lineNumber = getLineNumberFoo(test.foo(Short.parseShort("1"), Byte.parseByte("0"), new int[0],
                new ArrayList<String>(), new HashMap<String, String>()));
        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", lineNumber);
        Assert.assertNotNull("The description should not be null", desc);
        Assert.assertEquals("(SB[ILjava/util/List;Ljava/util/Map;)[Ljava/lang/StackTraceElement;", desc);

        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", -1);
        Assert.assertNull(desc);

        desc = MethodLineNumberMatcher.getMethodDescription(TestFileWithLineNumbers.class, "foo", 1001);
        Assert.assertNull(desc);
    }

    public static int getLineNumberFoo(StackTraceElement[] elements) {
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("foo")) {
                return current.getLineNumber();
            }
        }
        return 0;
    }

    public static int getLineNumberConstructor(TestFileWithLineNumbers test) {
        StackTraceElement[] elements = test.getTrace();
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("<init>")) {
                return current.getLineNumber();
            }
        }
        return 0;
    }

    public static int getLineNumberBar(StackTraceElement[] elements) {
        for (StackTraceElement current : elements) {
            if (current.getMethodName().equals("bar")) {
                return current.getLineNumber();
            }
        }
        return 0;
    }

    @Test
    public void testInstrumentationWithMethodInfoTrace() {
        MyTestFile file = new MyTestFile();
        int lineNumber = getLineNumberBar(file.bar());
        MethodInfo info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        Map<String, Object> actual = verifyBasicsInstrumented(info, true);
        List<String> args = (List<String>) actual.get("args");
        Assert.assertEquals(0, args.size());
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Assert.assertTrue((Boolean) inst.get("dispatcher"));
        Map<String, List<String>> types = (Map<String, List<String>>) inst.get("types");
        Assert.assertNotNull(types);
        Assert.assertEquals(1, types.size());
        Assert.assertEquals(1, (types.get(InstrumentationType.TraceAnnotation.toString()).size()));
        Assert.assertEquals("MyTestFile.java", (types.get(InstrumentationType.TraceAnnotation.toString()).get(0)));
    }

    @Test
    public void testInstrumentationWithMethodInfoTraceNoDispatcher() {
        MyTestFile file = new MyTestFile();
        int lineNumber = getLineNumberBar(file.bar(6));
        MethodInfo info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        Map<String, Object> actual = verifyBasicsInstrumented(info, true);
        List<String> args = (List<String>) actual.get("args");
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("int", args.get(0));
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Assert.assertFalse((Boolean) inst.get("dispatcher"));
        Map<String, List<String>> types = (Map<String, List<String>>) inst.get("types");
        Assert.assertNotNull(types);
        Assert.assertEquals(1, types.size());
        Assert.assertEquals(1, (types.get(InstrumentationType.TraceAnnotation.toString()).size()));
        Assert.assertEquals("MyTestFile.java", (types.get(InstrumentationType.TraceAnnotation.toString()).get(0)));
    }

    @Test
    public void testInstrumentationIgnore() {
        MyTestFile file = new MyTestFile();
        int lineNumber = getLineNumberBar(file.bar(6));
        MethodInfo info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        Assert.assertNotNull(info);
        Assert.assertTrue(info instanceof ExactMethodInfo);
        List<Map<String, Object>> actual = info.getJsonMethodMaps();
        Assert.assertNotNull(actual);
        Assert.assertEquals(1, actual.size());
        Map<String, Object> methodData = actual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(2, methodData.size());
        List<String> args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.size());
    }

    @Test
    public void testInstrumentationWithRetransform() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"testing1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>" + MyTestFile.class.getName());
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>bar</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("<type>java.lang.Object</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        MyTestFile file = new MyTestFile();
        int lineNumber = getLineNumberBar(file.bar("12f", new Object()));
        MethodInfo info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        Map<String, Object> actual = verifyBasicsInstrumented(info, true);
        List<String> args = (List<String>) actual.get("args");
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Assert.assertTrue((Boolean) inst.get("dispatcher"));
        Map<String, List<String>> types = (Map<String, List<String>>) inst.get("types");
        Assert.assertNotNull(types);
        Assert.assertEquals(1, types.size());
        Assert.assertEquals(1, (types.get(InstrumentationType.RemoteCustomXml.toString()).size()));
        Assert.assertEquals("testing1", (types.get(InstrumentationType.RemoteCustomXml.toString()).get(0)));
    }

    @Test
    public void testInstrumentationWithRetransformAndTrace() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"testing2\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>" + MyTestFile.class.getName());
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>bar</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        MyTestFile file = new MyTestFile();
        int lineNumber = getLineNumberBar(file.bar("abcde"));
        MethodInfo info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        Map<String, Object> actual = verifyBasicsInstrumented(info, true);
        List<String> args = (List<String>) actual.get("args");
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Assert.assertTrue((Boolean) inst.get("dispatcher"));
        Map<String, List<String>> types = (Map<String, List<String>>) inst.get("types");
        Assert.assertNotNull(types);
        Assert.assertEquals(2, types.size());
        Assert.assertEquals(1, (types.get(InstrumentationType.RemoteCustomXml.toString()).size()));
        Assert.assertEquals("testing2", (types.get(InstrumentationType.RemoteCustomXml.toString()).get(0)));
        Assert.assertEquals(1, (types.get(InstrumentationType.RemoteCustomXml.toString()).size()));
        Assert.assertEquals("MyTestFile.java", (types.get(InstrumentationType.TraceAnnotation.toString()).get(0)));
    }

    @Test
    public void testPointCutWithReinstrument() {
        RpcCall rpcCall = new RpcCall();

        // reinstrument with more instrumentation
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"Not A Pointcut\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>test.newrelic.test.agent.RpcCall");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>invoke</name>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        MethodInfo info = MethodInfoUtil.createMethodInfo(test.newrelic.test.agent.RpcCall.class,
                "invoke", -1);
        Map<String, Object> actual = verifyBasicsInstrumented(info, false);
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Map<String, Object> types = (Map<String, Object>) inst.get("types");
        Assert.assertEquals(2, types.keySet().size());
        Assert.assertEquals("Not A Pointcut",
                ((List<String>) types.get(InstrumentationType.RemoteCustomXml.toString())).get(0));
        Assert.assertEquals("com.newrelic.agent.instrumentation.pointcuts.XmlRpcPointCut",
                ((List<String>) types.get(InstrumentationType.Pointcut.toString())).get(0));
    }

    @Test
    public void testWeaveWithReinstrument() {
        class JspTest implements HttpJspPage {

            @Override
            public void jspDestroy() {
            }

            @Override
            public void jspInit() {
            }

            @Override
            public void init(ServletConfig config) throws ServletException {
            }

            @Override
            public ServletConfig getServletConfig() {
                return null;
            }

            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            }

            @Override
            public String getServletInfo() {
                return null;
            }

            @Override
            public void destroy() {
            }

            @Override
            public void _jspService(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException,
                    IOException {
            }

        }

        JspTest test = new JspTest();

        // reinstrument with more instrumentation
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"Testing\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>" + JspTest.class.getName());
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>_jspService</name>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        MethodInfo info = MethodInfoUtil.createMethodInfo(JspTest.class, "_jspService", -1);
        Map<String, Object> actual = verifyBasicsInstrumented(info, false);
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Map<String, Object> types = (Map<String, Object>) inst.get("types");
        Assert.assertEquals(2, types.keySet().size());
        Assert.assertEquals("Testing",
                ((List<String>) types.get(InstrumentationType.RemoteCustomXml.toString())).get(0));
        Assert.assertEquals("com.newrelic.instrumentation.jsp-2.4",
                ((List<String>) types.get(InstrumentationType.TracedWeaveInstrumentation.toString())).get(0));

    }

    public static Map<String, Object> verifyBasicsInstrumented(MethodInfo info, boolean isMultiMethod) {
        Assert.assertNotNull(info);
        if (isMultiMethod) {
            Assert.assertTrue(info instanceof ExactMethodInfo);
        } else {
            Assert.assertTrue(info instanceof MultipleMethodInfo);
        }
        List<Map<String, Object>> actual = info.getJsonMethodMaps();
        Assert.assertNotNull(actual);
        if (isMultiMethod) {
            Assert.assertEquals(1, actual.size());
        } else {
            Assert.assertTrue(actual.size() > 0);
        }
        Map<String, Object> methodData = actual.get(0);
        Assert.assertNotNull(methodData);
        Assert.assertEquals(2, methodData.size());
        List<String> args = (List<String>) methodData.get("args");
        Assert.assertNotNull(args);
        Map<String, Object> inst = (Map<String, Object>) methodData.get("traced_instrumentation");
        Assert.assertNotNull(inst);
        return methodData;
    }

    @Test
    public void testInstrumentationWithRetransformOrMatcher() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"testing1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>" + MyTestFile.class.getName());
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>bar</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("<type>java.lang.Object</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("<method>");
        sb.append("<name>bar</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        MyTestFile file = new MyTestFile();
        int lineNumber = getLineNumberBar(file.bar("12f", new Object()));
        MethodInfo info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        Map<String, Object> actual = verifyBasicsInstrumented(info, true);
        List<String> args = (List<String>) actual.get("args");
        Assert.assertEquals(2, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));
        Map<String, Object> inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Assert.assertTrue((Boolean) inst.get("dispatcher"));
        Map<String, List<String>> types = (Map<String, List<String>>) inst.get("types");
        Assert.assertNotNull(types);
        Assert.assertEquals(1, types.size());
        Assert.assertEquals(1, (types.get(InstrumentationType.RemoteCustomXml.toString()).size()));
        Assert.assertEquals("testing1", (types.get(InstrumentationType.RemoteCustomXml.toString()).get(0)));

        file = new MyTestFile();
        lineNumber = getLineNumberBar(file.bar("12f"));
        info = MethodInfoUtil.createMethodInfo(MyTestFile.class, "bar", lineNumber);
        actual = verifyBasicsInstrumented(info, true);
        args = (List<String>) actual.get("args");
        Assert.assertEquals(1, args.size());
        Assert.assertEquals("java.lang.String", args.get(0));
        inst = (Map<String, Object>) actual.get("traced_instrumentation");
        Assert.assertTrue((Boolean) inst.get("dispatcher"));
        types = (Map<String, List<String>>) inst.get("types");
        Assert.assertNotNull(types);
        Assert.assertEquals(2, types.size());
        Assert.assertEquals(1, (types.get(InstrumentationType.RemoteCustomXml.toString()).size()));
        Assert.assertEquals("testing1", (types.get(InstrumentationType.RemoteCustomXml.toString()).get(0)));
        Assert.assertEquals(1, (types.get(InstrumentationType.RemoteCustomXml.toString()).size()));
        Assert.assertEquals("MyTestFile.java", (types.get(InstrumentationType.TraceAnnotation.toString()).get(0)));
    }

}
