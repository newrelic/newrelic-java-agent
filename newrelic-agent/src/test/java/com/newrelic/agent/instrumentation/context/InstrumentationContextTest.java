/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

}
