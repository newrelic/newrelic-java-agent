package com.newrelic.agent;

import static org.junit.Assert.assertEquals;

import org.apache.commons.cli.CommandLine;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.discovery.AttachOptions;
import com.newrelic.agent.discovery.JsonSerializer;
import com.newrelic.bootstrap.BootstrapAgentTest;

public class AttachOptionsImplTest {
    final JsonSerializer serializer;

    public AttachOptionsImplTest() {
        AttachOptions options = getAttachOptions();
        this.serializer = options.getSerializer();
    }

    @Test
    public void testJson() throws Exception {
        String json = serializer.serialize(ImmutableMap.of("pid", 5), false);
        assertEquals("{\"pid\":5}", json);
    }

    @Test
    public void testJsonWithEncoding() throws Exception {
        String json = serializer.serialize(ImmutableMap.of("pid", 5), true);
        assertEquals("eJyrVirITFGyMq0FAA8DAuk=", json);
        String decoded = BootstrapAgentTest.decodeAndDecompress(json);
        assertEquals("{\"pid\":5}", decoded);
    }

    public static AttachOptions getAttachOptions() {
        return new AttachOptionsImpl(Mockito.mock(CommandLine.class));
    }
}
