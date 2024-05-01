package io.opentelemetry.sdk.trace;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.TracedMethod;
import io.opentelemetry.api.common.AttributeKey;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttributeHelperTest {

    @Test
    public void testSetLongAttribute() {
        Agent agent = mockAgent();

        AttributeHelper.setAttribute(agent, AttributeKey.longKey("test"), 555L);
        verify(agent.getTracedMethod(), times(1)).addCustomAttribute("test", 555L);
    }

    @Test
    public void testSetDoubleAttribute() {
        Agent agent = mockAgent();

        AttributeHelper.setAttribute(agent, AttributeKey.doubleKey("test"), 555D);
        verify(agent.getTracedMethod(), times(1)).addCustomAttribute("test", 555D);
    }

    @Test
    public void testSeBooleanAttribute() {
        Agent agent = mockAgent();

        AttributeHelper.setAttribute(agent, AttributeKey.booleanKey("test"), true);
        verify(agent.getTracedMethod(), times(1)).addCustomAttribute("test", true);
    }

    @Test
    public void testSetStringAttribute() {
        Agent agent = mockAgent();

        AttributeHelper.setAttribute(agent, AttributeKey.stringKey("test"), "myvalue");
        verify(agent.getTracedMethod(), times(1)).addCustomAttribute("test", "myvalue");
    }

    static Agent mockAgent() {
        Agent agent = mock(Agent.class);
        TracedMethod method = mock(TracedMethod.class);
        when(agent.getTracedMethod()).thenReturn(method);
        return agent;
    }
}