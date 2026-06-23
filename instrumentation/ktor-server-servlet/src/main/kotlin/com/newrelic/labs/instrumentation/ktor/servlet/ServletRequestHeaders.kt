/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.labs.instrumentation.ktor.servlet

import com.newrelic.api.agent.HeaderType
import com.newrelic.api.agent.Headers
import javax.servlet.http.HttpServletRequest

class ServletRequestHeaders(private val request: HttpServletRequest) : Headers {

    override fun getHeaderType(): HeaderType = HeaderType.HTTP

    override fun getHeader(name: String): String? = request.getHeader(name)

    override fun getHeaders(name: String): MutableList<String> {
        val result = mutableListOf<String>()
        val headers = request.getHeaders(name) ?: return result
        while (headers.hasMoreElements()) {
            result.add(headers.nextElement())
        }
        return result
    }

    override fun setHeader(name: String, value: String) {}

    override fun addHeader(name: String, value: String) {}

    override fun getHeaderNames(): MutableCollection<String> {
        val result = mutableListOf<String>()
        val names = request.headerNames ?: return result
        while (names.hasMoreElements()) {
            result.add(names.nextElement())
        }
        return result
    }

    override fun containsHeader(name: String): Boolean = request.getHeader(name) != null
}
