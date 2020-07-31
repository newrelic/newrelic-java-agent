/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cache;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.MockHarvestService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.util.MethodCache;
import com.newrelic.agent.util.SingleClassLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CacheServiceTest {

    private static final String APP_NAME = "Unit Test";
    private static final String TEST1_CLASS_NAME = "com/newrelic/agent/cache/CacheServiceTest$Test1";
    private static final String TEST1_CLASS_NAME_INTERNAL = "com.newrelic.agent.cache.CacheServiceTest$Test1";
    private static final String GETFIELD1_METHOD_NAME = "getField1";
    private static final String GETFIELD1_METHOD_DESC = "()V";
    private static final String FIELD1_NAME = "field1";

    @Before
    public void beforeTest() throws Exception {
        Map<String, Object> configMap = createStagingMap();
        createServiceManager(configMap);
    }

    @After
    public void afterTest() throws Exception {
        ServiceManager serviceManager = ServiceFactory.getServiceManager();
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    private static Map<String, Object> createMap() {
        return new HashMap<>();
    }

    private Map<String, Object> createStagingMap() {
        Map<String, Object> configMap = createMap();
        configMap.put("host", "staging-collector.newrelic.com");
        configMap.put("license_key", "deadbeefcafebabe8675309babecafe1beefdead");
        configMap.put(AgentConfigImpl.APP_NAME, APP_NAME);
        return configMap;
    }

    private void createServiceManager(Map<String, Object> configMap) throws Exception {

        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);

        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        ConfigService configService = ConfigServiceFactory.createConfigService(config, configMap);
        serviceManager.setConfigService(configService);

        CacheService cacheService = new CacheService();
        serviceManager.setCacheService(cacheService);

        HarvestService harvestService = new MockHarvestService();
        serviceManager.setHarvestService(harvestService);

        cacheService.start();
        serviceManager.start();
    }

    @Test
    public void getMethodCacheBasic() throws Exception {
        CacheService cacheService = ServiceFactory.getCacheService();
        MethodCache methodCache = cacheService.getMethodCache(TEST1_CLASS_NAME, GETFIELD1_METHOD_NAME,
                GETFIELD1_METHOD_DESC);
        String expectedField = "field1";
        Test1 test1 = new Test1(expectedField);
        Class<?> clazz = test1.getClass();
        Method method = methodCache.getDeclaredMethod(clazz);
        String actualField = (String) method.invoke(test1);
        Assert.assertEquals(expectedField, actualField);
    }

    @Test
    public void getMethodCacheIdentity() throws Exception {
        CacheService cacheService = ServiceFactory.getCacheService();
        MethodCache methodCache = cacheService.getMethodCache(TEST1_CLASS_NAME, GETFIELD1_METHOD_NAME,
                GETFIELD1_METHOD_DESC);
        MethodCache methodCache2 = cacheService.getMethodCache(TEST1_CLASS_NAME, GETFIELD1_METHOD_NAME,
                GETFIELD1_METHOD_DESC);
        Assert.assertEquals(methodCache, methodCache2);
    }

    @Test
    public void getSingleClassLoaderBasic() throws Exception {
        CacheService cacheService = ServiceFactory.getCacheService();
        SingleClassLoader singleClassLoader = cacheService.getSingleClassLoader(TEST1_CLASS_NAME_INTERNAL);
        Class<?> expectedClass = Test1.class;
        Class<?> actualClass = singleClassLoader.loadClass(getClass().getClassLoader());
        Assert.assertEquals(expectedClass, actualClass);
    }

    @Test
    public void getSingleClassLoaderIdentity() throws Exception {
        CacheService cacheService = ServiceFactory.getCacheService();
        SingleClassLoader singleClassLoader = cacheService.getSingleClassLoader(TEST1_CLASS_NAME);
        SingleClassLoader singleClassLoader2 = cacheService.getSingleClassLoader(TEST1_CLASS_NAME);
        Assert.assertEquals(singleClassLoader, singleClassLoader2);
    }

    public static class Test1 {

        private String field1;

        public Test1(String field1) {
            this.field1 = field1;
        }

        public String getField1() {
            return field1;
        }

        public String getField1Args(Object arg1) {
            return field1;
        }

    }

}
