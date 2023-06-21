package com.newrelic.agent.discovery;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.logging.Level;

import com.google.common.hash.HashCode;
import org.junit.Test;
import org.mockito.Mockito;

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

    @Test
    public void testGetProcessId() {
        Level level = Mockito.mock(Level.class);
        StatusMessage statusMessage = new StatusMessage("string_ID", level, "string_label", "string_message");

        assertEquals("string_ID", statusMessage.getProcessId());
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
