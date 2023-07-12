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

    @Test
    public void testHashCode() {
        Level level = Mockito.mock(Level.class);
        StatusMessage statusMessage = new StatusMessage("string_ID", level, "string_label", "string_message");

        int hashCode = Objects.hash("string_ID", "string_label", level, "string_message");
        assertEquals(hashCode, statusMessage.hashCode());
    }

    @Test
    public void testToString() {
        Level level = Mockito.mock(Level.class);
        String label = "string_label";
        String message = "string_message";
        StatusMessage statusMessage = new StatusMessage("string_ID", level, label, message);

        assertEquals(TerminalColor.fromLevel(level).formatMessage(label, message), statusMessage.toString());
    }

    @Test
    public void testEquals() {
        Level level = Mockito.mock(Level.class);
        Level level2 = Mockito.mock(Level.class);
        StatusMessage statusMessage = new StatusMessage("string_ID", level, "string_label", "string_message");
        StatusMessage otherStatusMessage = new StatusMessage("other_string_ID", level, "other_string_label", "other_string_message");
        StatusMessage otherStatusMessage2 = new StatusMessage("string_ID", level, "string_label", "other_string_message");
        StatusMessage otherStatusMessage3 = new StatusMessage("string_ID", level, "other_string_label", "string_message");
        StatusMessage otherStatusMessage4 = new StatusMessage("string_ID", level2, "string_label", "other_string_message");

        assertTrue(statusMessage.equals(statusMessage));
        assertFalse(statusMessage.equals(null));
        assertFalse(statusMessage.equals("Some kind of Object"));
        assertFalse(statusMessage.equals(otherStatusMessage));
        assertFalse(statusMessage.equals(otherStatusMessage2));
        assertFalse(statusMessage.equals(otherStatusMessage3));
        assertFalse(statusMessage.equals(otherStatusMessage4));
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
