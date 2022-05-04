package com.nr.instrumentation.kotlin.coroutines

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.api.agent.Trace
import kotlinx.coroutines.*
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx", "com.newrelic.instrumentation"], configName = "distributed_tracing.yml")
class CoroutineTest {

    @Test
    fun testSimple() {
        // given
        val expected = 42

        // when
        val value = simple()

        // then
        val introspector = InstrumentationTestRunner.getIntrospector()
        assertEquals(expected, value)
        assertEquals("Expected transaction count", 1, introspector.getFinishedTransactionCount(1000))

        // and
        val name = introspector.transactionNames.iterator().next()
        val expectedName = "OtherTransaction/Custom/com.nr.instrumentation.kotlin.coroutines.CoroutineTest/simple"
        assertEquals("Expected transaction name", expectedName, name)
    }

    @Test
    fun testCancel() {
        // given
        val expected = 42

        // when
        val value = cancel()

        // then
        val introspector = InstrumentationTestRunner.getIntrospector()
        assertEquals(expected, value) // should still be expected value due to cancel and instrumentation should allow cancel gracefully
        assertEquals("Expected transaction count", 1, introspector.getFinishedTransactionCount(1000))

        // and
        val name = introspector.transactionNames.iterator().next()
        val expectedName = "OtherTransaction/Custom/com.nr.instrumentation.kotlin.coroutines.CoroutineTest/cancel"
        assertEquals("Expected transaction name", expectedName, name)
    }

    @Trace(dispatcher = true)
    fun simple(): Int = runBlocking  {
        val sum = AtomicInteger(0)
        doSomethingUsefulOne(sum)
        doSomethingUsefulTwo(sum)
        sum.get()
    }

    @Trace(dispatcher = true)
    fun cancel(): Int = runBlocking  {
        val sum = AtomicInteger(0)
        val job = launch {
            val tooLong = async { takesTooLong(sum) }
            val first = async { doSomethingUsefulOne(sum) }
            val second = async { doSomethingUsefulTwo(sum) }
            // these lines should not finish before 'cancel'
            sum.addAndGet(tooLong.await())
            sum.addAndGet(first.await())
            sum.addAndGet(second.await())
        }
        delay(200)
        job.cancel()
        job.join()
        sum.get()
    }

    private suspend fun doSomethingUsefulOne(i: AtomicInteger): Int {
        delay(50)
        return i.addAndGet(13)
    }

    private suspend fun doSomethingUsefulTwo(i: AtomicInteger): Int {
        delay(100)
        return i.addAndGet(29)
    }

    private suspend fun takesTooLong(i: AtomicInteger): Int {
        delay(5000L)
        return i.addAndGet(999)
    }
}

