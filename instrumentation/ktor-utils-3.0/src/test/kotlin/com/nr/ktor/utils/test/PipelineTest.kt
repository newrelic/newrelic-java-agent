/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.ktor.utils.test

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector
import com.newrelic.api.agent.Trace
import io.ktor.util.pipeline.Pipeline
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["io.ktor.util.pipeline.Pipeline", "com.newrelic.instrumentation.labs.ktor"])
class PipelineTest {

    private lateinit var introspector: Introspector

    @Before
    fun setup() {
        introspector = InstrumentationTestRunner.getIntrospector()
        introspector.clear()
    }

    @Test
    fun testTrackedPipelineExecuteCompletesTransaction() {
        runTrackedPipelineTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)
    }

    @Test
    fun testUntrackedPipelineExecuteCompletesTransaction() {
        runUntrackedPipelineTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)
    }

    @Trace(dispatcher = true)
    fun runTrackedPipelineTransaction() {
        val phase = PipelinePhase("Test")
        val pipeline = HttpResponsePipeline(phase)
        pipeline.intercept(phase) { proceed() }
        runBlocking {
            pipeline.execute(Unit, "subject")
        }
    }

    @Trace(dispatcher = true)
    fun runUntrackedPipelineTransaction() {
        val phase = PipelinePhase("Test")
        val pipeline = OtherPipeline(phase)
        pipeline.intercept(phase) { proceed() }
        runBlocking {
            pipeline.execute(Unit, "subject")
        }
    }

    private class HttpResponsePipeline(vararg phases: PipelinePhase) : Pipeline<Any, Any>(*phases)

    private class OtherPipeline(vararg phases: PipelinePhase) : Pipeline<Any, Any>(*phases)
}
