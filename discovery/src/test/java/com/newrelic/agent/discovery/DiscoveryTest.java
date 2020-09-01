package com.newrelic.agent.discovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.mockito.Mockito;

public class DiscoveryTest {
    @Test
    public void list() throws Exception {
        AttachOptions options = Mockito.mock(AttachOptions.class);
        Mockito.when(options.isList()).thenReturn(Boolean.TRUE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonAttachOutput attachOutput = JsonAttachOutputTest.createJsonAttachOutput(out);
        Mockito.when(options.getSerializer()).thenReturn(new DefaultJsonSerializer());
        Discovery.discover(options, attachOutput);

        assertFalse(out.toString().isEmpty());
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) new JSONParser().parse(out.toString());
        assertNotNull(list);
    }
}
