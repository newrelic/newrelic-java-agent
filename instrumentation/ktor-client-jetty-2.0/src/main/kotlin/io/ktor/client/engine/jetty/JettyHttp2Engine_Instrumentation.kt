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

/**
 * Instrumentation for Ktor's Jetty HTTP/2 client engine.
 *
 * This module instruments the JettyHttp2Engine class to provide external call
 * tracking for HTTP requests made through Jetty's HTTP/2 client.
 *
 * Note: New Relic does not have library-level instrumentation for Jetty HTTP/2 client,
 * so this Ktor-level instrumentation is essential for visibility.
 */
@Weave(type = MatchType.ExactClass, originalName = "io.ktor.client.engine.jetty.JettyHttp2Engine")
internal class JettyHttp2Engine_Instrumentation {

    /**
     * Instruments the execute method to track external HTTP calls.
     *
     * @Trace(leaf = true) prevents this segment from having children, which avoids
     * double-counting if the interface-level instrumentation is also present.
     */
    @Trace(leaf = true)
    public suspend fun execute(data: HttpRequestData): HttpResponseData {
        // Defensive null check and instrumentation (required for Kotlin suspend function Weave instrumentation)
        if (data != null) {
            try {
                // Extract request details
                val url = data.url
                val uri = URI.create(url.toString())
                val method = data.method.toString()

                // Note: DT headers are added at the Sender level in ktor-client-core-2.0
                // before the request reaches the engine

                // Build external parameters
                val params = HttpParameters.library("Ktor-Jetty")
                    .uri(uri)
                    .procedure(method)
                    .noInboundHeaders()
                    .build()

                // Report as external call
                NewRelic.getAgent().tracedMethod.reportAsExternal(params)
            } catch (e: Throwable) {
                NewRelic.getAgent().logger.log(
                    java.util.logging.Level.FINER,
                    "Error in JettyHttp2Engine instrumentation: ${e.message}"
                )
            }
        } else {
            NewRelic.getAgent().logger.log(
                java.util.logging.Level.FINER,
                "HttpRequestData is null in JettyHttp2Engine.execute() - skipping instrumentation"
            )
        }

        // Call original method (only one call allowed)
        return Weaver.callOriginal()
    }
}
