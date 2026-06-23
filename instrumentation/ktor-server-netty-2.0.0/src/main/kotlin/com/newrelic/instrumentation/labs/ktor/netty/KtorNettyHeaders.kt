/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.netty

import com.newrelic.api.agent.HeaderType
import com.newrelic.api.agent.Headers

class KtorNettyHeaders(private val ktorHeaders: io.ktor.http.Headers) : Headers {


    override fun getHeaderType(): HeaderType? {
        return HeaderType.HTTP
    }

    override fun getHeader(name: String): String? {
        val value = ktorHeaders.get(name)
        return value
    }

    override fun getHeaders(name: String): Collection<String?>? {
        val values = ktorHeaders.getAll(name)
        return values
    }

    override fun setHeader(p0: String?, p1: String?) {
    }

    override fun addHeader(p0: String?, p1: String?) {
    }

    override fun getHeaderNames(): Collection<String?>? {
       return ktorHeaders.names()
    }

    override fun containsHeader(name: String): Boolean {
        return ktorHeaders.contains(name)
    }
}