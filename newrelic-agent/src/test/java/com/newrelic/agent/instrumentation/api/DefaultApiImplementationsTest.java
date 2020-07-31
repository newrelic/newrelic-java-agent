/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.api;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class DefaultApiImplementationsTest {

    @Test
    public void test() throws Exception {
        DefaultApiImplementations impls = new DefaultApiImplementations();
        Map<String, Map<Method, MethodNode>> apiClassNameToDefaultMethods = impls.getApiClassNameToDefaultMethods();
        Assert.assertEquals(2, apiClassNameToDefaultMethods.size());
        Map<Method, MethodNode> map = apiClassNameToDefaultMethods.get(Type.getType(Response.class).getInternalName());
        Assert.assertNotNull(map);
        Assert.assertTrue(map.containsKey(new Method("getHeaderType", "()Lcom/newrelic/api/agent/HeaderType;")));
        Assert.assertTrue(map.containsKey(new Method("getContentType", "()Ljava/lang/String;")));

        map = apiClassNameToDefaultMethods.get(Type.getType(Request.class).getInternalName());
        Assert.assertNotNull(map);
        Assert.assertTrue(map.containsKey(new Method("getHeaderType", "()Lcom/newrelic/api/agent/HeaderType;")));
        Assert.assertTrue(map.containsKey(new Method("getRequestURI", "()Ljava/lang/String;")));
    }

    @Test
    public void abstractsNotAllowed() throws Exception {
        try {
            new DefaultApiImplementations(AbstractsNotAllowed.class);
        } catch (Exception ex) {
            Assert.assertEquals(
                    "com.newrelic.agent.instrumentation.api.DefaultApiImplementationsTest$AbstractsNotAllowed cannot be abstract",
                    ex.getMessage());
        }

    }

    private abstract static class AbstractsNotAllowed {

    }
}
