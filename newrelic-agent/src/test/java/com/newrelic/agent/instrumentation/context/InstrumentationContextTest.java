/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.commons.Method;

import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InstrumentationContextTest {

    @Test
    public void testInterfacesDoesntReturnNull() {
        InstrumentationContext testClass = new InstrumentationContext(null, null, null);
        assertNotNull(testClass.getInterfaces());
    }

    @Test
    public void testGetCodeSourceLocationHappyPath() throws Exception {
        URL url = URI.create("http://example.com").toURL();
        CodeSource source = new CodeSource(url, new Certificate[]{});
        ProtectionDomain domain = new ProtectionDomain(source, null);
        InstrumentationContext testClass = new InstrumentationContext(null, null, domain);
        assertEquals(url, testClass.getCodeSourceLocation());
    }

    @Test
    public void testGetCodeSourceLocationNull() throws Exception {
        InstrumentationContext testClass = new InstrumentationContext(null, null, null);
        assertNull(testClass.getCodeSourceLocation());

        ProtectionDomain domain = new ProtectionDomain(null, null);
        testClass = new InstrumentationContext(null, null, domain);
        assertNull(testClass.getCodeSourceLocation());

        CodeSource source = new CodeSource(null, new Certificate[]{});
        domain = new ProtectionDomain(source, null);
        testClass = new InstrumentationContext(null, null, domain);
        assertNull(testClass.getCodeSourceLocation());
    }

    @Test
    public void getAndSetInterfaces_withNoAddedInterfaces_returnsEmptyStringArray() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, null, null);
        assertEquals(0,instrumentationContext.getInterfaces().length);
    }

    @Test
    public void getAndSetInterfaces_withAddedInterfaces_returnsInterfacesArray() {
        String [] interfaces = {"1", "2", "3"};
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, null, null);
        instrumentationContext.setInterfaces(interfaces);
        assertEquals(3,instrumentationContext.getInterfaces().length);
        assertEquals("1",instrumentationContext.getInterfaces()[0]);
        assertEquals("2",instrumentationContext.getInterfaces()[1]);
        assertEquals("3",instrumentationContext.getInterfaces()[2]);
    }

    @Test
    public void getClassName_withoutCallingSetter_returnsNull() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, null, null);
        assertNull(instrumentationContext.getClassName());
    }

    @Test
    public void getClassName_callingSetter_returnsCallName() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, null, null);
        instrumentationContext.setClassName("java.lang.String");
        assertEquals("java.lang.String", instrumentationContext.getClassName());
    }

    @Test
    public void getClassBeingRedefined_returnsClassFromConstructor() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        assertEquals(String.class, instrumentationContext.getClassBeingRedefined());
    }

    @Test
    public void getProtectionDomain_returnsProtectionDomainFromConstructor() {
        CodeSource mockCodeSource = Mockito.mock(CodeSource.class);
        PermissionCollection mockPermissionCollections = Mockito.mock(PermissionCollection.class);
        ProtectionDomain protectionDomain = new ProtectionDomain(mockCodeSource, mockPermissionCollections);
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, protectionDomain);
        assertEquals(protectionDomain, instrumentationContext.getProtectionDomain());
    }

    @Test
    public void markAsModified_switchesModifiedFlagToTrue() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        instrumentationContext.markAsModified();
        assertTrue(instrumentationContext.isModified());
    }

    @Test
    public void getTraceInformation_withNoTraceInfoSupplied_returnsEmptyTraceInfoObject() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        assertEquals(0, instrumentationContext.getTraceInformation().getTraceAnnotations().size());
    }

    @Test
    public void getTraceInformation_withTraceInfoSupplied_returnsTraceInfoObject() {
        Method mockMethod = Mockito.mock(Method.class);
        TraceDetails mockTraceDetails = Mockito.mock(TraceDetails.class);
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        instrumentationContext.addTrace(mockMethod, mockTraceDetails);
        assertEquals(1, instrumentationContext.getTraceInformation().getTraceAnnotations().size());
    }

    @Test
    public void getWeavedMethods_withNoMethodsSupplied_returnsEmptySet() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        assertTrue(instrumentationContext.getWeavedMethods().isEmpty());
    }

    @Test
    public void getWeavedMethods_withMethodsSupplied_returnsPopulatedSet() {
        Method mockMethod = Mockito.mock(Method.class);
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        instrumentationContext.addWeavedMethod(mockMethod, "title");
        assertEquals(1, instrumentationContext.getWeavedMethods().size());
    }

    @Test
    public void getSkippedMethods_withNoMethodsSupplied_returnsEmptySet() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        assertTrue(instrumentationContext.getSkipMethods().isEmpty());
    }

    @Test
    public void getSkippedMethods_withMethodsSupplied_returnsPopulatedSet() {
        Method mockMethod = Mockito.mock(Method.class);
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        instrumentationContext.addSkipMethod(mockMethod, "owningClass");
        assertEquals(1, instrumentationContext.getSkipMethods().size());
    }

    @Test
    public void getScalaFinalFields_withNoFieldsSupplied_returnsEmptySet() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        assertTrue(instrumentationContext.getScalaFinalFields().isEmpty());
    }

    @Test
    public void getScalaFinalFields_withFieldsSupplied_returnsPopulatedSet() {
        InstrumentationContext instrumentationContext = new InstrumentationContext(null, String.class, null);
        instrumentationContext.addScalaFinalField("field");
        assertEquals(1, instrumentationContext.getScalaFinalFields().size());
    }

}