package com.nr.instrumentation.kotlin.coroutines

import com.newrelic.agent.introspec.*
import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import kotlinx.coroutines.*
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Ignore
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

    @Test
    fun simpleCoroutineTest() {
        val introspector = InstrumentationTestRunner.getIntrospector()
        val result = runBlocking {
            txn {
                val i = trace("segment1") { 1 }
                val j = trace("segment2") { 2 }
                i + j
            }
        }
        val txnCount = introspector.getFinishedTransactionCount()
        val traces = getTraces(introspector)
        val segments = getSegments(traces)

        assertEquals("result correct", 3, result)
        assertEquals("transaction finished", 1, txnCount)
        assertSegmentPresent("segment1", segments)
        assertSegmentPresent("segment2", segments)
    }

    @Ignore
    @Test
    fun threadSwitchCoroutineTest() {
        val introspector = InstrumentationTestRunner.getIntrospector()
        val result = runBlocking {
            txn {
                val i = withContext(Dispatchers.IO){trace("segment1") { 1 }}
                val j = trace("segment2") { 2 }
                i + j
            }
        }
        val txnCount = introspector.getFinishedTransactionCount()
        val traces = getTraces(introspector)
        val segments = getSegments(traces)

        assertEquals("result correct", 3, result)
        assertEquals("transaction finished", 1, txnCount)
        assertSegmentPresent("segment1", segments)
        assertSegmentPresent("segment2", segments)
    }

    @Trace(dispatcher = true)
    suspend fun <S> txn(block: suspend () -> S): S {
        val txn = NewRelic.getAgent().transaction
        return block()
    }

    suspend fun <S> trace(segmentName: String, block: suspend () -> S): S {
        val txn = NewRelic.getAgent().getTransaction()
        println("Thread ${Thread.currentThread().name}")
        val segment = txn.startSegment(segmentName)
        val evaluatedBlock = block()
        segment.end()
        return evaluatedBlock
    }

    fun getTraces(introspector: Introspector): Iterable<TransactionTrace> =
            introspector.transactionNames.flatMap {
                introspector.getTransactionTracesForTransaction(it)
            }

    fun getSegments(traces: Iterable<TransactionTrace>): List<TraceSegment> =
            traces.flatMap { txnTrace -> getSegments(txnTrace.initialTraceSegment) }

    fun getSegments(segment: TraceSegment): List<TraceSegment> {
        val childSegments = segment.children.flatMap { getSegments(it) }
        return childSegments + segment
    }

    fun assertSegmentPresent(segmentName: String, segments: List<TraceSegment>) {
        val segment = segments.find { it.name == "Custom/$segmentName" }
        Assert.assertNotNull("segment $segmentName not found", segment)
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

