package com.newrelic.instrumentation.kotlin.coroutines

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx", "com.newrelic.instrumentation"], configName = "distributed_tracing.yml")
class CoroutineTests {

    val cut = CoroutineClass()

    @Test
    fun testIt() {
        runBlocking {
            val i = 42
            assertTrue(i == 42)
            cut.runIt()
        }
    }
}

