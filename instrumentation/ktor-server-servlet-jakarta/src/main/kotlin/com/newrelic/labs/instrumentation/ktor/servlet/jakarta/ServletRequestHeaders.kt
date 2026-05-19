/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.servlet.jakarta

import com.newrelic.api.agent.HeaderType
import com.newrelic.api.agent.Headers
import jakarta.servlet.http.HttpServletRequest

/**
 * Wrapper for HttpServletRequest to adapt it to New Relic's Headers interface.
 * This allows the New Relic agent to extract distributed tracing headers from incoming HTTP requests.
 */
class ServletRequestHeaders(private val request: HttpServletRequest) : Headers {

    override fun getHeaderType(): HeaderType {
        return HeaderType.HTTP
    }

    override fun getHeader(name: String): String? {
        return request.getHeader(name)
    }

    override fun getHeaders(name: String): MutableList<String> {
        val headers = request.getHeaders(name)
        val result = mutableListOf<String>()
        if (headers != null) {
            while (headers.hasMoreElements()) {
                result.add(headers.nextElement())
            }
        }
        return result
    }

    override fun setHeader(name: String, value: String) {
        // Request headers are read-only, cannot set
    }

    override fun addHeader(name: String, value: String) {
        // Request headers are read-only, cannot add
    }

    override fun getHeaderNames(): MutableCollection<String> {
        val names = request.headerNames
        val result = mutableListOf<String>()
        if (names != null) {
            while (names.hasMoreElements()) {
                result.add(names.nextElement())
            }
        }
        return result
    }

    override fun containsHeader(name: String): Boolean {
        return request.getHeader(name) != null
    }
}
