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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["io.ktor"])
class PipelineTest {

    private lateinit var introspector: Introspector

    @Before
    fun setup() {
        introspector = InstrumentationTestRunner.getIntrospector()
        introspector.clear()
    }

    @Test
    fun testPipelineExecuteIsTraced() {
        runPipelineTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)
        val txnName = introspector.transactionNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)
        assertTrue(
            "Pipeline.execute should produce a Custom/Ktor-Utils metric",
            metrics.keys.any { it.contains("Ktor-Utils") && it.contains("execute") }
        )
    }

    @Test
    fun testPipelineExecuteMetricIncludesClassName() {
        runPipelineTransaction()

        val txnName = introspector.transactionNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)
        assertTrue(
            "Metric should include the concrete pipeline class name",
            metrics.keys.any { it.contains("Pipeline") && it.contains("execute") }
        )
    }

    @Trace(dispatcher = true)
    fun runPipelineTransaction() {
        val phase = PipelinePhase("Test")
        val pipeline = Pipeline<String, Unit>(phase)
        pipeline.intercept(phase) { proceed() }
        runBlocking {
            pipeline.execute(Unit, "subject")
        }
    }
}
