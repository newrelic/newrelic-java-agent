/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.client.engine.jetty

import com.newrelic.api.agent.HttpParameters
import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.MatchType
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import java.net.URI

@Weave(type = MatchType.ExactClass, originalName = "io.ktor.client.engine.jetty.JettyHttp2Engine")
internal class JettyHttp2Engine_Instrumentation {

    @Trace(leaf = true)
    public suspend fun execute(data: HttpRequestData): HttpResponseData {
        if (data != null) {
            try {
                val uri = URI.create(data.url.toString())
                val params = HttpParameters.library("Ktor-Jetty")
                    .uri(uri)
                    .procedure(data.method.toString())
                    .noInboundHeaders()
                    .build()
                NewRelic.getAgent().tracedMethod.reportAsExternal(params)
            } catch (e: Throwable) {
                NewRelic.getAgent().logger.log(
                    java.util.logging.Level.FINER,
                    "Error in JettyHttp2Engine instrumentation: ${e.message}"
                )
            }
        }
        return Weaver.callOriginal()
    }
}
