/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package javax.servlet.http;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.sling.NewRelicOutputStreamWrapper;
import com.nr.agent.instrumentation.sling.NewRelicPrintWriterWrapper;
import com.nr.agent.instrumentation.sling.SlingUtils;

import javax.servlet.ServletOutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;

import static com.nr.agent.instrumentation.sling.SlingUtils.BROWSER_MONITORING_CONFIG_KEY;
import static com.nr.agent.instrumentation.sling.SlingUtils.HTML_MIME_TYPE;

@Weave(originalName = "javax.servlet.ServletResponse", type = MatchType.Interface)
public abstract class ServletResponse_Instrumented {
    @NewField
    private NewRelicPrintWriterWrapper writerWrapper;

    @NewField
    private NewRelicOutputStreamWrapper outputStreamWrapper;

    public PrintWriter getWriter() {
        PrintWriter originalPrintWriter = Weaver.callOriginal();
        boolean browserMonitoringEnabled = NewRelic.getAgent().getConfig()
                .getValue(BROWSER_MONITORING_CONFIG_KEY, Boolean.TRUE);

        if (browserMonitoringEnabled) {
            String contentType = getContentType();

            if (contentType != null && contentType.startsWith(HTML_MIME_TYPE)) {
                if (writerWrapper == null) {
                    if (originalPrintWriter.getClass().getName().equals(SlingUtils.PRINT_WRITER_CLASS_NAME)) {
                        // Sling wraps the response for includes; the delegate response's getWriter()
                        // has already returned a NewRelicPrintWriterWrapper. Reuse it to avoid
                        // creating another layer of wrapping. String comparison is used instead of
                        // instanceof because OSGi classloader isolation can cause instanceof to return
                        // false even when the class name matches.
                        return originalPrintWriter;
                    }
                    AgentBridge.getAgent().getLogger().log(Level.FINEST,
                            "[Sling-BM] Wrapping PrintWriter for browser monitoring: response={0}, writer={1}",
                            this.getClass().getName(), originalPrintWriter.getClass().getName());
                    writerWrapper = new NewRelicPrintWriterWrapper(originalPrintWriter);
                }

                return writerWrapper;
            }
        }

        return originalPrintWriter;
    }

    public ServletOutputStream getOutputStream() {
        ServletOutputStream originalOutputStream = Weaver.callOriginal();
        boolean browserMonitoringEnabled = NewRelic.getAgent().getConfig()
                .getValue(BROWSER_MONITORING_CONFIG_KEY, Boolean.TRUE);

        if (browserMonitoringEnabled) {
            String contentType = getContentType();

            if (contentType != null && contentType.startsWith(HTML_MIME_TYPE)) {
                if (outputStreamWrapper == null) {
                    if (originalOutputStream.getClass().getName().equals(SlingUtils.OUTPUT_STREAM_CLASS_NAME)) {
                        // Same OSGi classloader reasoning as getWriter() above.
                        return originalOutputStream;
                    } else {
                        AgentBridge.getAgent().getLogger().log(Level.FINEST,
                                "[Sling-BM] Wrapping OutputStream for browser monitoring: response={0}, stream={1}",
                                this.getClass().getName(), originalOutputStream.getClass().getName());
                        outputStreamWrapper = new NewRelicOutputStreamWrapper(originalOutputStream);
                    }
                }
                return outputStreamWrapper;
            }
        }

        return originalOutputStream;
    }

    public void setContentLength(int len) {
        String contentType = this.getContentType();

        // For HTML, our injection changes the payload size so suppress Content-Length
        // to force a chunked transfer encoding.
        if (NewRelic.getAgent().getConfig()
                .getValue(BROWSER_MONITORING_CONFIG_KEY, Boolean.TRUE) &&
                contentType != null && contentType.startsWith(HTML_MIME_TYPE)) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "[Sling-BM] Suppressing setContentLength({0}) for content-type={1}, response={2}",
                    len, contentType, this.getClass().getName());
            return;
        }

        Weaver.callOriginal();
    }

    public void setContentLengthLong(long len) {
        String contentType = this.getContentType();

        if (NewRelic.getAgent().getConfig()
                .getValue(BROWSER_MONITORING_CONFIG_KEY, Boolean.TRUE) &&
                contentType != null && contentType.startsWith(HTML_MIME_TYPE)) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "[Sling-BM] Suppressing setContentLengthLong({0}) for content-type={1}, response={2}",
                    len, contentType, this.getClass().getName());
            return;
        }

        Weaver.callOriginal();
    }

    public abstract String getContentType();
}