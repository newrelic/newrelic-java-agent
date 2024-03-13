package com.newrelic.agent.bridge;


import com.newrelic.api.agent.weaver.internal.WeavePackageType;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
public class AgentBridgeTest {

    @Test
    public void getAgent_returnsNoOpAgent() {
        assertEquals(NoOpAgent.INSTANCE, AgentBridge.getAgent());
    }

    @Test
    public void extensionHolderFactoryInstance_returnsNoOpInstance() {
        assertEquals(ExtensionHolderFactory.NoOpExtensionHolderFactory.class, AgentBridge.extensionHolderFactory.getClass());
    }

    @Test
    public void testCurrentApiSourceThreadLocalInstance() {

        ThreadLocal<WeavePackageType> instance = AgentBridge.currentApiSource;

        instance.set(WeavePackageType.FIELD);
        assertEquals(WeavePackageType.FIELD, instance.get());

        instance.remove();
        assertEquals(WeavePackageType.UNKNOWN, instance.get());

        instance.set(null);
        assertEquals(WeavePackageType.UNKNOWN, instance.get());
    }

    @Test
    public void testTokenAndRefCount() {
        Token mockToken = Mockito.mock(Token.class);
        TracedMethod mockTracedMethod = Mockito.mock(TracedMethod.class);
        AgentBridge.TokenAndRefCount instance = new AgentBridge.TokenAndRefCount(mockToken, mockTracedMethod, new AtomicInteger(1));

        assertEquals(mockToken, instance.token);
        assertEquals(mockTracedMethod, instance.tracedMethod.get());
        assertEquals(1, instance.refCount.get());

        instance = new AgentBridge.TokenAndRefCount(null, mockTracedMethod, new AtomicInteger(1));
        assertEquals(NoOpToken.INSTANCE, instance.token);

        // Just to get a coverage hit
        ThreadLocal<AgentBridge.TokenAndRefCount> dummy = AgentBridge.activeToken;


    }
}
