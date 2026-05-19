/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.netty

import com.newrelic.api.agent.HeaderType
import com.newrelic.api.agent.Headers
import java.util.Collections

class KtorNettyHeaders(private val nettyHeaders: io.netty.handler.codec.http.HttpHeaders) : Headers {


    override fun getHeaderType(): HeaderType? {
        return HeaderType.HTTP
    }

    override fun getHeader(name: String): String? {
        val value = nettyHeaders.get(name)
        return value
    }

    override fun getHeaders(name: String): Collection<String?>? {
        val values = nettyHeaders.getAll(name)
        if (values != null) {
            return values
        } else {
            return Collections.emptyList()
        }
    }

    override fun setHeader(p0: String?, p1: String?) {
    }

    override fun addHeader(p0: String?, p1: String?) {
    }

    override fun getHeaderNames(): Collection<String?>? {
       return nettyHeaders.names()
    }

    override fun containsHeader(name: String): Boolean {
        return nettyHeaders.contains(name)
    }
}