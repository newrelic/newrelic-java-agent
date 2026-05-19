/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sling;

import com.newrelic.api.agent.NewRelic;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NewRelicOutputStreamWrapper extends ServletOutputStream {
    private final ServletOutputStream originalStream;
    private boolean hasInjected = false;

    public NewRelicOutputStreamWrapper(ServletOutputStream originalStream) {
        this.originalStream = originalStream;
    }

    @Override
    public boolean isReady() {
        return originalStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        originalStream.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException {
        if (hasInjected) {
            originalStream.write(b);
            return;
        }
        byte[] singleByte = { (byte) b };
        writeThrough(singleByte, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (hasInjected) {
            originalStream.write(b, off, len);
            return;
        }
        writeThrough(b, off, len);
    }

    /**
     * Scans the incoming data for a <head> tag and injects the browser monitoring script
     * immediately after it. All data passes directly to the underlying stream. Once injected,
     * hasInjected is set and the public write() overrides skip this method entirely.
     */
    private void writeThrough(byte[] b, int off, int len) throws IOException {
        String data = new String(b, off, len, StandardCharsets.ISO_8859_1);

        int idx = data.indexOf("<head>");
        if (idx < 0) {
            idx = data.indexOf("<HEAD>");
        }

        if (idx >= 0) {
            int headEnd = idx + 6;
            originalStream.write(data.substring(0, headEnd).getBytes(StandardCharsets.ISO_8859_1));
            String scriptPayload = NewRelic.getBrowserTimingHeader();
            if (scriptPayload != null && !scriptPayload.isEmpty()) {
                originalStream.write(scriptPayload.getBytes(StandardCharsets.ISO_8859_1));
                SlingUtils.logTransactionDetails();
            }
            if (headEnd < data.length()) {
                originalStream.write(data.substring(headEnd).getBytes(StandardCharsets.ISO_8859_1));
            }
            hasInjected = true;
        } else {
            // Write original bytes directly — no re-encoding
            originalStream.write(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        originalStream.flush();
    }

    @Override
    public void close() throws IOException {
        originalStream.close();
    }

    boolean hasInjected() {
        return hasInjected;
    }
}