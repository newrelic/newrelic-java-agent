package com.newrelic.agent.service.module;

import com.newrelic.agent.bridge.TestLogger;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ClassNoticingFactoryTest {
    @Test
    public void noticingJunit() throws IOException {
        ExecutorService executorService = mock(ExecutorService.class);

        JarAnalystFactory factory = mock(JarAnalystFactory.class);
        when(factory.createURLAnalyzer(any(URL.class))).thenReturn(mock(Runnable.class));

        ClassNoticingFactory target = new ClassNoticingFactory(factory, executorService, new TestLogger());

        target.newClassMatchVisitor(
                Test.class.getClassLoader(),
                Test.class,
                new ClassReader("org.junit.Test"),
                new ClassWriter(0),
                new InstrumentationContext(null, Test.class, Test.class.getProtectionDomain())
        );

        ArgumentCaptor<URL> captor = ArgumentCaptor.forClass(URL.class);
        verify(factory, times(1)).createURLAnalyzer(captor.capture());

        // Yes, this test will fail if you upgrade junit. Should be an easy fix, but sorry!
        assertTrue(captor.getValue().toString(), captor.getValue().toString().endsWith("junit-4.12.jar"));
    }

    @Test
    public void noticingTestClass() throws IOException {
        ExecutorService executorService = mock(ExecutorService.class);

        JarAnalystFactory factory = mock(JarAnalystFactory.class);
        when(factory.createURLAnalyzer(any(URL.class))).thenReturn(mock(Runnable.class));

        ClassNoticingFactory target = new ClassNoticingFactory(factory, executorService, new TestLogger());

        target.newClassMatchVisitor(
                this.getClass().getClassLoader(),
                this.getClass(),
                new ClassReader(this.getClass().getName()),
                new ClassWriter(0),
                new InstrumentationContext(null, this.getClass(), this.getClass().getProtectionDomain())
        );

        String codeSourceLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
        if (codeSourceLocation.endsWith("/")) {
            // we expect the test classes to be staged in a gradle directory, not in a jar.
            verifyNoMoreInteractions(factory, executorService);
        } else {
            fail("Unexpected jar for test class?? " + codeSourceLocation);
        }
    }


    @Test
    public void doesNotAddDuplicates() throws MalformedURLException {
        ExecutorService executorService = mock(ExecutorService.class);

        JarAnalystFactory factory = mock(JarAnalystFactory.class);
        when(factory.createURLAnalyzer(any(URL.class))).thenReturn(mock(Runnable.class));

        ClassNoticingFactory target = new ClassNoticingFactory(factory, executorService, new TestLogger());

        target.addURL(new URL(
                "file:/Users/roger/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.1.jar!/org/apache/commons/httpclient/HttpVersion.class"));
        target.addURL(new URL(
                "file:/Users/roger/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.1.jar!/org/apache/commons/httpclient/Dude.class"));

        verify(factory, times(1)).createURLAnalyzer(new URL("file:/Users/roger/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.1.jar"));
        verify(executorService, times(1)).submit(any(Runnable.class));
    }

    @Test
    public void jarProtocol() throws MalformedURLException {
        ExecutorService executorService = mock(ExecutorService.class);

        JarAnalystFactory factory = mock(JarAnalystFactory.class);
        when(factory.createURLAnalyzer(any(URL.class))).thenReturn(mock(Runnable.class));

        ClassNoticingFactory target = new ClassNoticingFactory(factory, executorService, new TestLogger());

        // jboss sends us complex urls like this
        target.addURL(new URL(
                "jar:file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar!/"));

        verify(factory, times(1)).createURLAnalyzer(new URL("file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar"));
        verify(executorService, times(1)).submit(any(Runnable.class));
    }
}
