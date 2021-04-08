package com.newrelic.agent.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;

import org.junit.Test;

public class StatusMessageTest {
    @Test
    public void testError() throws IOException {
        final StatusMessage message = StatusMessage.error("1", "Error", "Bad news");
        verifySerialization(message);
        assertEquals("Error", message.getLabel());
        assertEquals("Bad news", message.getMessage());
        assertEquals(Level.SEVERE, message.getLevel());
        assertFalse(message.isSuccess());
    }

    @Test
    public void testInfo() throws IOException {
        final StatusMessage message = StatusMessage.info("1", "Info", "Test");
        verifySerialization(message);
        assertEquals("Info", message.getLabel());
        assertEquals("Test", message.getMessage());
        assertEquals(Level.INFO, message.getLevel());
        assertFalse(message.isSuccess());
    }

    @Test
    public void testWarn() throws IOException {
        final StatusMessage message = StatusMessage.warn("1", "Warning", "Danger");
        verifySerialization(message);
        assertEquals("Warning", message.getLabel());
        assertEquals("Danger", message.getMessage());
        assertEquals(Level.WARNING, message.getLevel());
        assertFalse(message.isSuccess());
    }

    @Test
    public void testUrl() throws IOException {
        final StatusMessage message = StatusMessage.success("1", "http://localhost");
        verifySerialization(message);
        assertEquals("Url", message.getLabel());
        assertEquals("http://localhost", message.getMessage());
        assertEquals(Level.INFO, message.getLevel());
        assertTrue(message.isSuccess());
    }

    private void verifySerialization(StatusMessage message) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream dataOut = new ObjectOutputStream(out)) {
            message.writeExternal(dataOut);
        }
        final StatusMessage message2;
        try (ObjectInputStream dataIn =
                new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            message2 = StatusMessage.readExternal(dataIn);
        }
        assertEquals(message, message2);
        assertEquals(message.getLabel(), message2.getLabel());
        assertEquals(message.getMessage(), message2.getMessage());
        assertEquals(message.getLevel(), message2.getLevel());
    }
}
