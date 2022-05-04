package com.newrelic.instrumentation.kotlin.coroutines

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx", "com.newrelic.instrumentation"], configName = "distributed_tracing.yml")
class CoroutinTests {

    //val cut = CoroutineClass()

    @Test
    fun testIt() {
        val i = 42
        assertTrue(i == 42)
        //cut.runIt()
    }
}

