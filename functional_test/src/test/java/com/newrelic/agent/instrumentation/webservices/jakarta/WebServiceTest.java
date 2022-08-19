/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.webservices.jakarta;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebServiceTest {
    static final TransactionDataList transactions = new TransactionDataList();

    @BeforeClass
    public static void beforeClass() {
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
    }

    @Before
    public void setup() {
        transactions.clear();
    }

    @Test
    public void testTracedInterfaceMethods() throws NoSuchMethodException, SecurityException {
        Assert.assertTrue(HelloWorldImpl.class.isAnnotationPresent(InstrumentedClass.class));

        Method method = HelloWorldImpl.class.getMethod("run");
        Assert.assertNotNull(method);
        Assert.assertFalse(method.isAnnotationPresent(InstrumentedMethod.class));

        method = HelloWorldImpl.class.getMethod("getHelloWorld", String.class);
        Assert.assertNotNull(method);
        Assert.assertTrue(method.isAnnotationPresent(InstrumentedMethod.class));
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertEquals("com.newrelic.agent.instrumentation.webservices.JakartaWebServiceVisitor",
                annotation.instrumentationNames()[0]);
        Assert.assertEquals(InstrumentationType.BuiltIn, annotation.instrumentationTypes()[0]);
    }

    @Test
    public void testTracedMethods() throws NoSuchMethodException, SecurityException {
        Assert.assertTrue(JakartaWsExample.class.isAnnotationPresent(InstrumentedClass.class));

        Method method = JakartaWsExample.class.getMethod("run");
        Assert.assertNotNull(method);
        Assert.assertFalse(method.isAnnotationPresent(InstrumentedMethod.class));

        method = JakartaWsExample.class.getMethod("getWebMethod", String.class);
        Assert.assertNotNull(method);
        Assert.assertTrue(method.isAnnotationPresent(InstrumentedMethod.class));
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertEquals("com.newrelic.agent.instrumentation.webservices.JakartaWebServiceVisitor",
                annotation.instrumentationNames()[0]);
        Assert.assertEquals(InstrumentationType.BuiltIn, annotation.instrumentationTypes()[0]);
    }

    @Ignore("This test is flaky, regularly causing the build to fail non-deterministically. Can't reproduce locally.")
    // Might be caused by firewall issues.
    @Test
    public void test() throws MalformedURLException, InterruptedException {
        Endpoint endpoint = Endpoint.publish("http://localhost:8670/ws/hello", new HelloWorldImpl());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        endpoint.setExecutor(executor);
        try {
            URL url = new URL("http://localhost:8670/ws/hello?wsdl");

            QName qname = new QName("http://webservices.instrumentation.agent.newrelic.com/", "HelloWorldImplService");

            Service service = Service.create(url, qname);
            service.setExecutor(executor);

            HelloWorld hello = service.getPort(HelloWorld.class);

            Assert.assertEquals("Hey what's up Dude", hello.getHelloWorld("Dude"));

        } finally {
            endpoint.stop();
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Assert.assertEquals(2, transactions.waitFor(2, 15000).size());

        Set<String> metrics = transactions.getMetricNameStrings();
        AgentHelper.verifyMetrics(metrics,
                "WebTransaction/WebService/com.newrelic.agent.instrumentation.webservices.jakarta.HelloWorld/getHelloWorld",
                "Java/com.sun.xml.internal.ws.transport.http.server.WSHttpHandler/handle", "HttpDispatcher",
                "Java/com.newrelic.agent.instrumentation.webservices.jakarta.HelloWorldImpl/getHelloWorld");

    }

    @Ignore("This test is flaky, regularly causing the build to fail non-deterministically. Can't reproduce locally.")
    // Might be caused by firewall issues.
    @Test
    public void testProvider() throws Exception {
        Endpoint endpoint = Endpoint.publish("http://localhost:8675/provider", new SimpleClientServiceImpl());
        try {
            URL url = new URL("http://localhost:8675/provider");

            QName qname = new QName("http://webservices.instrumentation.agent.newrelic.com/", "SimpleClientService");

            Service service = Service.create(url, qname);

            Iterator<QName> ports = service.getPorts();
            QName name = ports.next();

            // service.addPort(name, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:8675/provider");
            Dispatch<Source> sourceDispatch = service.createDispatch(name, Source.class, Service.Mode.PAYLOAD);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.appendChild(doc.createElement("dude"));

            Source source = new DOMSource(doc);
            Source result = sourceDispatch.invoke(source);
            Assert.assertNotNull(result);

        } finally {
            endpoint.stop();
        }

        Set<String> metrics = transactions.waitFor(4, 10).getMetricNameStrings();
        AgentHelper.verifyMetrics(
                metrics,
                "WebTransaction/Custom/com.newrelic.agent.instrumentation.webservices.jakarta.SimpleClientServiceImpl/invoke", // problematic
                "Java/com.sun.xml.internal.ws.transport.http.server.WSHttpHandler/handle", "HttpDispatcher",
                "Java/com.newrelic.agent.instrumentation.webservices.jakarta.SimpleClientServiceImpl/invoke"); // problematic
    }
}
