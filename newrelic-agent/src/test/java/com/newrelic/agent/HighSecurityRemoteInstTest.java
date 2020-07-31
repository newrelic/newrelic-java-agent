/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.reinstrument.ReinstrumentResult;
import com.newrelic.agent.reinstrument.RemoteInstrumentationServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class HighSecurityRemoteInstTest {

    private static final String APP_NAME = "NAME";
    private MockServiceManager manager;

    @Before
    public void setup() {
        try {
            manager = new MockServiceManager();
            ServiceFactory.setServiceManager(manager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RemoteInstrumentationServiceImpl performSetupWork(boolean highSecurity) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("app_name", APP_NAME);
        if (highSecurity) {
            settings.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
        } else {
            settings.put(AgentConfigImpl.HIGH_SECURITY, Boolean.FALSE);
        }

        manager.setConfigService(new ConfigServiceFactory().createConfigServiceUsingSettings(settings));

        TransactionService transactionService = new TransactionService();
        manager.setTransactionService(transactionService);
        MockCoreService agent = new MockCoreService();
        manager.setCoreService(agent);
        agent.setInstrumentation(new InstrumentationProxy(new TestInstrumentation(), false));

        // Needed by Transaction
        TransactionTraceService transactionTraceService = new TransactionTraceService();
        manager.setTransactionTraceService(transactionTraceService);

        manager.setAttributesService(new AttributesService());

        // Needed by Transaction
        MockRPMServiceManager rpmServiceManager = new MockRPMServiceManager();
        manager.setRPMServiceManager(rpmServiceManager);
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceManager.setRPMService(rpmService);

        RemoteInstrumentationServiceImpl impl = new RemoteInstrumentationServiceImpl();
        manager.setReinstrumentService(impl);
        return impl;
    }

    private ReinstrumentResult runTest(boolean highSecurity) {
        RemoteInstrumentationServiceImpl impl = performSetupWork(highSecurity);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"testing 123\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObj2");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getName</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        return impl.processXml(sb.toString());
    }

    @Test
    public void testHighSecurityRemoteInstTest() {

        ReinstrumentResult result = runTest(true);

        Assert.assertNotNull(result.getStatusMap().get("errors"));
        String errors = (String) result.getStatusMap().get("errors");
        Assert.assertTrue("Error does not contain high security. Error: " + errors,
                errors.contains("not supported in high security"));
        Assert.assertEquals(0, result.getStatusMap().get("pointcuts_specified"));

    }

    @Test
    public void testNoHighSecurityRemoteInstTest() throws Exception {
        ReinstrumentResult result = runTest(false);
        // this is going to have an error, but the point cut count will get set which should not happen in high security
        Assert.assertEquals(1, result.getStatusMap().get("pointcuts_specified"));

    }

    class TestInstrumentation implements Instrumentation {
        @Override
        public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {

        }

        @Override
        public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {

        }

        @Override
        public boolean removeTransformer(ClassFileTransformer transformer) {
            return false;
        }

        @Override
        public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
                UnmodifiableClassException {

        }

        @Override
        public boolean isRetransformClassesSupported() {
            return true;
        }

        @Override
        public boolean isRedefineClassesSupported() {
            return true;
        }

        @Override
        public boolean isNativeMethodPrefixSupported() {
            return false;
        }

        @Override
        public boolean isModifiableClass(Class<?> theClass) {
            return false;
        }

        @Override
        public long getObjectSize(Object objectToSize) {
            return 0;
        }

        @Override
        public Class[] getInitiatedClasses(ClassLoader loader) {
            return null;
        }

        @Override
        public Class[] getAllLoadedClasses() {
            return null;
        }

        @Override
        public void appendToSystemClassLoaderSearch(JarFile jarfile) {

        }

        @Override
        public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {

        }

        @Override
        public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {

        }

        @Override
        public void addTransformer(ClassFileTransformer transformer) {

        }
    }
}
