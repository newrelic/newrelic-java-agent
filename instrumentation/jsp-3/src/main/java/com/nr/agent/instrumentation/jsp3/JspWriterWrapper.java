/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jsp3;

import com.newrelic.api.agent.NewRelic;

import jakarta.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JspWriterWrapper extends JspWriter {

    private static final Pattern HEAD_PATTERN = Pattern.compile("<head.*>", Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
    private JspWriter originalWriter;

    private boolean found = false;

    public JspWriterWrapper(JspWriter originalWriter)  {
        this(-1, false);
        this.originalWriter = originalWriter;
    }

    protected JspWriterWrapper(int bufferSize, boolean autoFlush) {
        super(bufferSize, autoFlush);
    }

    @Override
    public void newLine() throws IOException {
        originalWriter.newLine();
    }

    @Override
    public void print(boolean b) throws IOException {
        originalWriter.print(b);
    }

    @Override
    public void print(char c) throws IOException {
        originalWriter.print(c);
    }

    @Override
    public void print(int i) throws IOException {
        originalWriter.print(i);
    }

    @Override
    public void print(long l) throws IOException {
        originalWriter.print(l);
    }

    @Override
    public void print(float f) throws IOException {
        originalWriter.print(f);
    }

    @Override
    public void print(double d) throws IOException {
        originalWriter.print(d);
    }

    @Override
    public void print(char[] s) throws IOException {
        originalWriter.print(s);
    }

    @Override
    public void print(String str) throws IOException {
        if (!found) {
            originalWriter.print(detectAndModifyHeadElement(str));
        } else {
            originalWriter.print(str);
        }
    }

    @Override
    public void print(Object obj) throws IOException {
        originalWriter.print(obj);
    }

    @Override
    public void println() throws IOException {
        originalWriter.println();
    }

    @Override
    public void println(boolean b) throws IOException {
        originalWriter.println(b);
    }

    @Override
    public void println(char c) throws IOException {
        originalWriter.println(c);
    }

    @Override
    public void println(int i) throws IOException {
        originalWriter.println(i);
    }

    @Override
    public void println(long l) throws IOException {
        originalWriter.println(l);
    }

    @Override
    public void println(float f) throws IOException {
        originalWriter.println(f);
    }

    @Override
    public void println(double d) throws IOException {
        originalWriter.println(d);
    }

    @Override
    public void println(char[] c) throws IOException {
        originalWriter.println(c);
    }

    @Override
    public void println(String str) throws IOException {
        if (!found) {
            originalWriter.println(detectAndModifyHeadElement(str));
        } else {
            originalWriter.println(str);
        }
    }

    @Override
    public void println(Object o) throws IOException {
        originalWriter.println(o);
    }

    @Override
    public void clear() throws IOException {
        originalWriter.clear();
    }

    @Override
    public void clearBuffer() throws IOException {
        originalWriter.clearBuffer();
    }

    @Override
    public void write(int c) throws IOException {
        originalWriter.write(c);
    }

    @Override
    public void write(String s, int off, int len) throws IOException {
        originalWriter.write(s, off, len);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        originalWriter.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        originalWriter.flush();
    }

    @Override
    public void close() throws IOException {
        originalWriter.close();
    }

    @Override
    public int getRemaining() {
        return originalWriter.getRemaining();
    }

    private String detectAndModifyHeadElement(String originalContent) {
        String modifiedContent = null;
        Matcher m = HEAD_PATTERN.matcher(originalContent);
        if (m.find()) {
            // The getBrowserTimingHeader call performs the "does transaction exist" check so not duplicating that here
            modifiedContent = originalContent.substring(0, m.end()) + NewRelic.getBrowserTimingHeader() + originalContent.substring(m.end());
            found = true;
        }

        return modifiedContent != null ? modifiedContent : originalContent;
    }
}
