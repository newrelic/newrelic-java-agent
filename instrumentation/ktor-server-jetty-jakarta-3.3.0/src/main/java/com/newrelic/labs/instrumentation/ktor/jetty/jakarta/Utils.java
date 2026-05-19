/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.jetty.jakarta;

import org.eclipse.jetty.server.Request;
import com.newrelic.api.agent.TracedMethod;

public class Utils {

    /**
     * Enhanced transaction naming using Jetty request information
     * Combines URI path, method, and optionally host information
     */
    public static String getEnhancedTransactionName(String uri, String method, String host, String scheme) {
        StringBuilder sb = new StringBuilder();
        
        // Add URI path
        if (uri != null && !uri.isEmpty()) {
            // Remove query parameters for cleaner transaction names
            String path = uri.split("\\?")[0];
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.isEmpty()) {
                sb.append("Root");
            } else {
                // Limit path segments to avoid too granular transaction names
                String[] segments = path.split("/");
                if (segments.length > 3) {
                    sb.append(segments[0]).append("/").append(segments[1]).append("/").append(segments[2]).append("/*");
                } else {
                    sb.append(path);
                }
            }
        } else {
            sb.append("Unknown");
        }
        
        // Add HTTP method
        if (method != null && !method.isEmpty()) {
            sb.append(" (").append(method).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Extracts useful attributes from a Jetty request for New Relic tracking
     */
    public static void addRequestAttributes(Request request, TracedMethod tracedMethod) {
        if (request == null || tracedMethod == null) {
            return;
        }
        
        try {
            // Basic request info
            String uri = request.getHttpURI().getPath();
            String method = request.getMethod();
            String scheme = request.getHttpURI().getScheme();
            String host = request.getHttpURI().getHost();
            
            if (uri != null) tracedMethod.addCustomAttribute("Request-URI", uri);
            if (method != null) tracedMethod.addCustomAttribute("Request-Method", method);
            if (scheme != null) tracedMethod.addCustomAttribute("Request-Scheme", scheme);
            if (host != null) tracedMethod.addCustomAttribute("Request-Host", host);
            
            // Connection info (using static methods from Request class)
            try {
                String localAddr = Request.getLocalAddr(request);
                String remoteAddr = Request.getRemoteAddr(request);
                int localPort = Request.getLocalPort(request);
                int remotePort = Request.getRemotePort(request);
                
                if (localAddr != null) tracedMethod.addCustomAttribute("Local-Address", localAddr);
                if (remoteAddr != null) tracedMethod.addCustomAttribute("Remote-Address", remoteAddr);
                tracedMethod.addCustomAttribute("Local-Port", localPort);
                tracedMethod.addCustomAttribute("Remote-Port", remotePort);
            } catch (Exception e) {
                // Ignore connection info errors - not critical
            }
            
            // HTTP version
            try {
                String httpVersion = request.getConnectionMetaData().getHttpVersion().asString();
                if (httpVersion != null) {
                    tracedMethod.addCustomAttribute("HTTP-Version", httpVersion);
                }
            } catch (Exception e) {
                // Ignore version errors - not critical
            }
            
        } catch (Exception e) {
            // Don't let attribute extraction break the main flow
        }
    }
}