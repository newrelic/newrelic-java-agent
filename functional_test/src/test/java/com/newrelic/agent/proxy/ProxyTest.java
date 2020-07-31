/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.proxy;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.asm.Utils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThat;

public class ProxyTest {

    private ProxyClassFileTransformer transformer;

    private String getInternalName(Class<?> clazz) {
        return clazz.getName().replaceAll("[.]", "/");
    }

    @Before
    public void init() {
        transformer = new ProxyClassFileTransformer();
        ServiceFactory.getCoreService().getInstrumentation().addTransformer(transformer);
    }

    @After
    public void end() {
        ServiceFactory.getCoreService().getInstrumentation().removeTransformer(transformer);
    }

    @Test
    public void testIsProxy() {
        MyProxyInterface t = (MyProxyInterface) Proxy.newProxyInstance(MyProxyInterface.class.getClassLoader(),
                new Class<?>[] { MyProxyInterface.class }, new MyProxyHandler(new MyProxyImpl()));

        t.getSomeString();
        t.getSomeInt();

        assertThat(transformer.proxyClassNames, Matchers.hasItem(getInternalName(t.getClass())));
        assertThat(transformer.nonProxyClassNames, Matchers.hasItem(getInternalName(MyProxyInterface.class)));
        assertThat(transformer.nonProxyClassNames, Matchers.hasItem(getInternalName(MyProxyImpl.class)));
    }

    static class ProxyClassFileTransformer implements ClassFileTransformer {

        List<String> proxyClassNames;
        List<String> nonProxyClassNames;

        public ProxyClassFileTransformer() {
            proxyClassNames = new ArrayList<>();
            nonProxyClassNames = new ArrayList<>();
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws ClassFormatError {
            if (className.contains("Proxy")) {
                if (Utils.isJdkProxy(classfileBuffer)) {
                    proxyClassNames.add(className);
                } else {
                    nonProxyClassNames.add(className);
                }
            }
            return classfileBuffer;
        }

    }
}
