package com.newrelic.agent.instrumentation;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.weave.utils.Streams;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class InstrumentationUtilsTest {

    @Test
    public void test_isAbleToResolveAgent_exception() throws Throwable {
        ClassLoader myClassLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(myClassLoader.loadClass(Mockito.anyString())).thenThrow(ClassNotFoundException.class);
        Assert.assertFalse(InstrumentationUtils
                .isAbleToResolveAgent(myClassLoader, null));
    }

    @Test
    public void test_generateClassBytesWithSerialVersionUID() throws IOException {
        Map<String, Object> root = new HashMap<>();
        MockServiceManager mockServiceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(Mockito.mock(ConfigService.class));

        AgentConfig config = AgentConfigImpl.createAgentConfig(root);
        Mockito.when(ServiceFactory.getConfigService().getDefaultAgentConfig()).thenReturn(config);

        byte[] classBytes = getClassBytesFromClassLoaderResource(InstrumentUtilsTest.class.getName(),
                ClassLoader.getSystemClassLoader());
        byte[] result = InstrumentationUtils.generateClassBytesWithSerialVersionUID(classBytes,
                0, InstrumentationUtils.class.getClassLoader());

        Assert.assertNotEquals(classBytes, result);
    }

    public static byte[] getClassBytesFromClassLoaderResource(String classname, ClassLoader classloader)
            throws IOException {
        InputStream is = classloader.getResourceAsStream(classname.replace('.', '/') + ".class");
        if (null == is) {
            return null; // no such resource
        }

        return Streams.read(is, true);
    }
}
