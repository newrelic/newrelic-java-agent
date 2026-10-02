/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.otlp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Minimal protobuf (proto3) wire format writer covering the subset of field types used by OTLP.
 * <p>
 * Every write method emits the field unconditionally. Callers decide when to omit default values, because
 * fields inside a {@code oneof} (such as {@code AnyValue}) must be written even when they hold a default value.
 * <p>
 * Nested messages are written in place: {@link #startMessage(int)} reserves space for the largest possible length
 * prefix and {@link #endMessage()} writes the actual length, shifting the message content left if the length
 * needed fewer bytes than were reserved.
 * <p>
 * This class is not thread safe.
 */
final class ProtobufWriter {

    private static final int WIRE_TYPE_VARINT = 0;
    private static final int WIRE_TYPE_FIXED64 = 1;
    private static final int WIRE_TYPE_LENGTH_DELIMITED = 2;
    private static final int WIRE_TYPE_FIXED32 = 5;

    // A message length is a non-negative int, which needs at most 5 bytes as a varint
    private static final int MAX_LENGTH_PREFIX_SIZE = 5;

    private byte[] buffer;
    private int size;
    private int[] messageStarts = new int[8];
    private int depth;

    ProtobufWriter(int initialCapacity) {
        buffer = new byte[Math.max(initialCapacity, 16)];
    }

    void writeVarint(int fieldNumber, long value) {
        writeTag(fieldNumber, WIRE_TYPE_VARINT);
        writeRawVarint(value);
    }

    void writeBool(int fieldNumber, boolean value) {
        writeVarint(fieldNumber, value ? 1 : 0);
    }

    void writeFixed64(int fieldNumber, long value) {
        writeTag(fieldNumber, WIRE_TYPE_FIXED64);
        ensureCapacity(8);
        for (int i = 0; i < 8; i++) {
            buffer[size++] = (byte) (value >>> (8 * i));
        }
    }

    void writeFixed32(int fieldNumber, int value) {
        writeTag(fieldNumber, WIRE_TYPE_FIXED32);
        ensureCapacity(4);
        for (int i = 0; i < 4; i++) {
            buffer[size++] = (byte) (value >>> (8 * i));
        }
    }

    void writeDouble(int fieldNumber, double value) {
        writeFixed64(fieldNumber, Double.doubleToRawLongBits(value));
    }

    void writeString(int fieldNumber, String value) {
        writeBytes(fieldNumber, value.getBytes(StandardCharsets.UTF_8));
    }

    void writeBytes(int fieldNumber, byte[] value) {
        writeTag(fieldNumber, WIRE_TYPE_LENGTH_DELIMITED);
        writeRawVarint(value.length);
        ensureCapacity(value.length);
        System.arraycopy(value, 0, buffer, size, value.length);
        size += value.length;
    }

    /**
     * Starts a nested message field. Every call must be paired with a call to {@link #endMessage()}.
     */
    void startMessage(int fieldNumber) {
        writeTag(fieldNumber, WIRE_TYPE_LENGTH_DELIMITED);
        ensureCapacity(MAX_LENGTH_PREFIX_SIZE);
        size += MAX_LENGTH_PREFIX_SIZE;
        if (depth == messageStarts.length) {
            messageStarts = Arrays.copyOf(messageStarts, depth * 2);
        }
        messageStarts[depth++] = size;
    }

    void endMessage() {
        if (depth == 0) {
            throw new IllegalStateException("endMessage() called without a matching startMessage()");
        }
        int contentStart = messageStarts[--depth];
        int contentLength = size - contentStart;
        int prefixStart = contentStart - MAX_LENGTH_PREFIX_SIZE;
        int prefixSize = varintSize(contentLength);

        if (prefixSize < MAX_LENGTH_PREFIX_SIZE) {
            System.arraycopy(buffer, contentStart, buffer, prefixStart + prefixSize, contentLength);
        }
        size = prefixStart;
        writeRawVarint(contentLength);
        size += contentLength;
    }

    int size() {
        return size;
    }

    byte[] toByteArray() {
        if (depth != 0) {
            throw new IllegalStateException(depth + " nested message(s) were started but not ended");
        }
        return Arrays.copyOf(buffer, size);
    }

    private void writeTag(int fieldNumber, int wireType) {
        writeRawVarint(((long) fieldNumber << 3) | wireType);
    }

    private void writeRawVarint(long value) {
        ensureCapacity(10);
        while ((value & ~0x7FL) != 0) {
            buffer[size++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer[size++] = (byte) value;
    }

    private static int varintSize(int value) {
        int bytes = 1;
        while ((value & ~0x7F) != 0) {
            bytes++;
            value >>>= 7;
        }
        return bytes;
    }

    private void ensureCapacity(int additionalBytes) {
        int required = size + additionalBytes;
        if (required > buffer.length) {
            buffer = Arrays.copyOf(buffer, Math.max(required, buffer.length * 2));
        }
    }
}
