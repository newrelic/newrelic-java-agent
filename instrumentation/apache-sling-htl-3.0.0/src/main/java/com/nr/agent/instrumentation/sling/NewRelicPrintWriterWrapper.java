/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.sling;

import com.newrelic.api.agent.NewRelic;

import java.io.PrintWriter;

public class NewRelicPrintWriterWrapper extends PrintWriter {
    private final PrintWriter originalWriter;
    private boolean hasInjected = false;

    public NewRelicPrintWriterWrapper(PrintWriter originalWriter) {
        super(originalWriter);
        this.originalWriter = originalWriter;
    }

    @Override
    public void write(char[] charBuf) {
        write(charBuf, 0, charBuf.length);
    }

    @Override
    public void write(char[] charBuf, int off, int len) {
        if (hasInjected) {
            originalWriter.write(charBuf, off, len);
            return;
        }
        writeThrough(new String(charBuf, off, len));
    }

    @Override
    public void write(int c) {
        if (hasInjected) {
            originalWriter.write(c);
            return;
        }
        writeThrough(String.valueOf((char) c));
    }

    @Override
    public void write(String str) {
        if (hasInjected) {
            originalWriter.write(str);
            return;
        }
        writeThrough(str);
    }

    @Override
    public void write(String str, int off, int len) {
        if (hasInjected) {
            originalWriter.write(str, off, len);
            return;
        }
        writeThrough(str.substring(off, off + len));
    }

    /**
     * Scans the incoming data for a <head> tag and injects the browser monitoring script
     * immediately after it. All data passes directly to the underlying writer. Once injected,
     * hasInjected is set and the public write() overrides skip this method entirely.
     */
    private void writeThrough(String data) {
        int idx = data.indexOf("<head>");
        if (idx < 0) {
            idx = data.indexOf("<HEAD>");
        }

        if (idx >= 0) {
            int headEnd = idx + 6;
            originalWriter.write(data.substring(0, headEnd));
            String scriptPayload = NewRelic.getBrowserTimingHeader();
            if (scriptPayload != null && !scriptPayload.isEmpty()) {
                originalWriter.write(scriptPayload);
                SlingUtils.logTransactionDetails();
            }

            if (headEnd < data.length()) {
                originalWriter.write(data.substring(headEnd));
            }
            hasInjected = true;
        } else {
            originalWriter.write(data);
        }
    }

    boolean hasInjected() {
        return hasInjected;
    }
}