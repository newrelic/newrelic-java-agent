package com.newrelic.bootstrap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.AttachOptionsImplTest;

public class BootstrapAgentTest {
    @Test
    public void testAttachArgs() throws IOException {
        String serialized = AttachOptionsImplTest.getAttachOptions().getSerializer().serialize(
                ImmutableMap.of("a", "b"), true);
        String json = BootstrapAgent.decodeAndDecompressAgentArguments(serialized);
        assertEquals("{\"a\":\"b\"}", json);
    }
}
