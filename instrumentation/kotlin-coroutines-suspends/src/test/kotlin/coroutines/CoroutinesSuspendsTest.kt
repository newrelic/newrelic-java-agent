package coroutines

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector
import com.newrelic.api.agent.Trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx.coroutines", "kotlin.coroutines", "com.newrelic.instrumentation.kotlin"])
class CoroutinesSuspendsTest {

    private lateinit var introspector: Introspector
    private val SUSPEND_NAME = "Custom/Kotlin/Coroutines/SuspendFunction/Continuation at coroutines.CoroutinesSuspendsTest\$checkSuspend\$1.invokeSuspend(CoroutinesSuspendsTest.kt)"

    @Before
    fun setup() {
        introspector = InstrumentationTestRunner.getIntrospector()
        introspector.clear()
    }

    /**
     * Run test to ensure that the suspend function gets traced.
     */
    @Test
    fun testSuspendFunction() {
        checkSuspend()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should track coroutines on custom suspend function", metrics.isNotEmpty())
        assertTrue("Suspend Function was not instrumented", metrics.keys.contains(SUSPEND_NAME))

    }


    @Trace(dispatcher = true)
    fun checkSuspend() {
        runBlocking {
            my_suspend()
        }
    }

    suspend fun my_suspend() {
        delay(100)
    }

}