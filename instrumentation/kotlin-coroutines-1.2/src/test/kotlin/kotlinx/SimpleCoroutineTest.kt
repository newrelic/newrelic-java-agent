package kotlinx

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector
import com.newrelic.agent.introspec.TraceSegment
import com.newrelic.agent.introspec.TransactionTrace
import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx"])
class SimpleCoroutineTest {

    @Test
    fun simpleCoroutineTest() {
        val introspector: Introspector = InstrumentationTestRunner.getIntrospector()
        val result = runBlocking {
            txn {
                    val i = trace("segment1") { 1 }
                    val j = trace("segment2") { 2 }
                    i + j
                }
            }
        val txnCount = introspector.getFinishedTransactionCount()
        val traces = introspector.getTraces()
        val segments = getSegments(traces)

        Assert.assertEquals("result correct", 3, result)
        Assert.assertEquals("transaction finished", 1, txnCount)
        assertSegmentPresent("segment1", segments)
        assertSegmentPresent("segment2", segments)
    }

    @Test
    fun ThreadSwitchCoroutineTest() {
        val introspector: Introspector = InstrumentationTestRunner.getIntrospector()
        val result = runBlocking {
            txn {
                val i = withContext(IO){trace("segment1") { 1 }}
                val j = trace("segment2") { 2 }
                i + j
            }
        }
        val txnCount = introspector.getFinishedTransactionCount()
        val traces = introspector.getTraces()
        val segments = getSegments(traces)

        Assert.assertEquals("result correct", 3, result)
        Assert.assertEquals("transaction finished", 1, txnCount)
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

    fun Introspector.getTraces(): Iterable<TransactionTrace> =
        transactionNames.flatMap {
            getTransactionTracesForTransaction(it)
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


}