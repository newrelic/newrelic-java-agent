/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProtobufWriterTest {

    @Test
    public void emptyWriterProducesEmptyMessage() {
        assertArrayEquals(new byte[0], new ProtobufWriter(0).toByteArray());
    }

    @Test
    public void varintsMatchProtobufEncoding() throws Exception {
        long[] values = { 0, 1, 127, 128, 300, 16383, 16384, Integer.MAX_VALUE, Long.MAX_VALUE, -1 };
        ProtobufWriter writer = new ProtobufWriter(4);
        for (long value : values) {
            writer.writeVarint(1, value);
        }

        CodedInputStream input = CodedInputStream.newInstance(writer.toByteArray());
        for (long value : values) {
            assertEquals(tag(1, WireFormat.WIRETYPE_VARINT), input.readTag());
            assertEquals(value, input.readInt64());
        }
        assertTrue(input.isAtEnd());
    }

    @Test
    public void fixedWidthAndLengthDelimitedFields() throws Exception {
        ProtobufWriter writer = new ProtobufWriter(4);
        writer.writeFixed64(1, 1_700_000_000_123_456_789L);
        writer.writeFixed32(2, 0x1FF);
        writer.writeDouble(3, 1.5);
        writer.writeBool(4, true);
        writer.writeString(5, "héllo");
        writer.writeBytes(6, new byte[] { 1, 2, 3 });

        CodedInputStream input = CodedInputStream.newInstance(writer.toByteArray());
        assertEquals(tag(1, WireFormat.WIRETYPE_FIXED64), input.readTag());
        assertEquals(1_700_000_000_123_456_789L, input.readFixed64());
        assertEquals(tag(2, WireFormat.WIRETYPE_FIXED32), input.readTag());
        assertEquals(0x1FF, input.readFixed32());
        assertEquals(tag(3, WireFormat.WIRETYPE_FIXED64), input.readTag());
        assertEquals(1.5, input.readDouble(), 0);
        assertEquals(tag(4, WireFormat.WIRETYPE_VARINT), input.readTag());
        assertTrue(input.readBool());
        assertEquals(tag(5, WireFormat.WIRETYPE_LENGTH_DELIMITED), input.readTag());
        assertEquals("héllo", input.readString());
        assertEquals(tag(6, WireFormat.WIRETYPE_LENGTH_DELIMITED), input.readTag());
        assertArrayEquals(new byte[] { 1, 2, 3 }, input.readByteArray());
        assertTrue(input.isAtEnd());
    }

    @Test
    public void nestedMessagesHaveCorrectLengthPrefixes() throws Exception {
        // 200 bytes of content needs a 2 byte length prefix, exercising the shift of the reserved prefix space
        char[] chars = new char[200];
        Arrays.fill(chars, 'a');
        String longValue = new String(chars);

        ProtobufWriter writer = new ProtobufWriter(4);
        writer.startMessage(1);
        writer.startMessage(2);
        writer.writeString(3, longValue);
        writer.endMessage();
        writer.startMessage(4);
        writer.endMessage();
        writer.endMessage();
        writer.writeVarint(5, 7);

        CodedInputStream outer = CodedInputStream.newInstance(writer.toByteArray());
        assertEquals(tag(1, WireFormat.WIRETYPE_LENGTH_DELIMITED), outer.readTag());
        CodedInputStream message = CodedInputStream.newInstance(outer.readByteArray());
        assertEquals(tag(5, WireFormat.WIRETYPE_VARINT), outer.readTag());
        assertEquals(7, outer.readInt64());
        assertTrue(outer.isAtEnd());

        assertEquals(tag(2, WireFormat.WIRETYPE_LENGTH_DELIMITED), message.readTag());
        CodedInputStream inner = CodedInputStream.newInstance(message.readByteArray());
        assertEquals(tag(4, WireFormat.WIRETYPE_LENGTH_DELIMITED), message.readTag());
        assertEquals(0, message.readByteArray().length);
        assertTrue(message.isAtEnd());

        assertEquals(tag(3, WireFormat.WIRETYPE_LENGTH_DELIMITED), inner.readTag());
        assertEquals(longValue, new String(inner.readByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void deeplyNestedMessagesGrowTheStack() throws Exception {
        ProtobufWriter writer = new ProtobufWriter(4);
        int depth = 20;
        for (int i = 0; i < depth; i++) {
            writer.startMessage(1);
        }
        writer.writeVarint(2, 42);
        for (int i = 0; i < depth; i++) {
            writer.endMessage();
        }

        byte[] bytes = writer.toByteArray();
        for (int i = 0; i < depth; i++) {
            CodedInputStream input = CodedInputStream.newInstance(bytes);
            assertEquals(tag(1, WireFormat.WIRETYPE_LENGTH_DELIMITED), input.readTag());
            bytes = input.readByteArray();
        }
        CodedInputStream input = CodedInputStream.newInstance(bytes);
        assertEquals(tag(2, WireFormat.WIRETYPE_VARINT), input.readTag());
        assertEquals(42, input.readInt64());
    }

    private static int tag(int fieldNumber, int wireType) {
        return (fieldNumber << 3) | wireType;
    }

    @Test(expected = IllegalStateException.class)
    public void endMessageWithoutStartMessageFails() {
        new ProtobufWriter(4).endMessage();
    }

    @Test(expected = IllegalStateException.class)
    public void toByteArrayWithUnclosedMessageFails() {
        ProtobufWriter writer = new ProtobufWriter(4);
        writer.startMessage(1);
        writer.toByteArray();
    }
}
