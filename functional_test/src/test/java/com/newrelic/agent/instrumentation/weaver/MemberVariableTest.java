/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExtensionHolder;
import com.newrelic.agent.bridge.ExtensionHolderFactory;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.instrument.UnmodifiableClassException;

public class MemberVariableTest {
    private VariableTest test;
    private static ExtensionHolderFactory originalExtensionFactory;
    private static ExtensionHolderFactory customExtensionFactory;
    public static int numExtensionClassesBuilt = 0;

    @BeforeClass
    public static void setup() throws Exception {
        originalExtensionFactory = AgentBridge.extensionHolderFactory;
        customExtensionFactory = new TestExtensionHolderFactory(originalExtensionFactory);
        AgentBridge.extensionHolderFactory = customExtensionFactory;
        FunctionalWeaveTestUtils.addClassChecker();
        init();
    }

    @AfterClass
    public static void afterClass() {
        Assert.assertEquals("AgentBridge.extensionHolderFactory has been altered outside of this test.",
                AgentBridge.extensionHolderFactory, customExtensionFactory);
        AgentBridge.extensionHolderFactory = originalExtensionFactory;
    }

    public static class TestExtensionHolderFactory implements ExtensionHolderFactory {
        private final ExtensionHolderFactory original;

        public TestExtensionHolderFactory(ExtensionHolderFactory original) {
            this.original = original;
        }

        @Override
        public <T> ExtensionHolder<T> build() {
            numExtensionClassesBuilt++;
            return original.build();
        }
    }

    @Before
    public void before() {
        test = new VariableTest();
    }

    @Test
    public void testCustomExtensionHolder() {
        test.doubleMemberVariable(4);
        Assert.assertEquals("AgentBridge extensionHolderFactory not hooked up.", 1, numExtensionClassesBuilt);
    }

    private static void init() throws Exception {
        FunctionalWeaveTestUtils.reinstrument(VariableTest.class.getName());
    }

    @Test
    public void retransform() throws UnmodifiableClassException {
        ServiceFactory.getCoreService().getInstrumentation().retransformClasses(VariableTest.class);
        testObjectReturn();
    }

    @Test
    public void testObjectReturn() {
        Assert.assertEquals("dude", test.test());
    }

    @Test
    public void testUseConstantNpe() {
        VariableTest test = new VariableTest();
        Assert.assertEquals("Test", test.useConstant(null));
    }

    @Test
    public void testUseConstant() {
        Assert.assertEquals("dude", test.useConstant(ImmutableMap.<String, Object>of("test", "dude")));
        Assert.assertEquals("Test", test.useConstant(ImmutableMap.<String, Object>of("testing", "dude")));
    }

    @Test
    public void testMemberVariable() {
        Assert.assertEquals(0, test.memberVariable(5));
        Assert.assertEquals(5, test.memberVariable(66));
        Assert.assertEquals(66, test.memberVariable(-1));
    }

    @Test
    public void testDoubleMemberVariable() {
        Assert.assertEquals(0d, test.doubleMemberVariable(5), 0);
        Assert.assertEquals(5d, test.doubleMemberVariable(66), 0);
        Assert.assertEquals(66d, test.doubleMemberVariable(-1), 0);
    }

}
