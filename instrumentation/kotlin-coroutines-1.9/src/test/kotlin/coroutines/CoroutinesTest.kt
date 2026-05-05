package coroutines

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector
import com.newrelic.api.agent.Trace
import com.newrelic.instrumentation.kotlin.coroutines_19.Utils
import com.nr.instrumentation.kotlin.coroutines.test.TestUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx.coroutines", "kotlin.coroutines", "com.newrelic.instrumentation.kotlin"])
class CoroutinesTest {

    private lateinit var introspector: Introspector

    @Before
    fun setup() {
        introspector = InstrumentationTestRunner.getIntrospector()
        introspector.clear()
    }

    @Test
    fun testRunBlocking() {
        runBlockingTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())

        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)
        assertTrue(
            "Should contain runBlocking metric",
            metrics.keys.any { it.contains("runBlocking") })

    }

    @Test
    fun testRunBlockingWithCoroutineName() {
        runBlockingWithName()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue(
            "Should contain runBlocking metric with name",
            metrics.keys.any { it.contains("runBlocking") && it.contains("TestName") })
    }

    @Test
    fun testLaunchBasic() {
        val latch = CountDownLatch(1)
        launchTransaction(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1
        )

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue(
            "Should contain launch metric",
            metrics.keys.any { it.contains("launch") })
    }

    @Test
    fun testLaunchWithCoroutineName() {
        val latch = CountDownLatch(1)
        launchWithName(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1
        )

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue(
            "Should contain launch metric with coroutine name",
            metrics.keys.any { it.contains("launch") && it.contains("MyLaunchCoroutine") })
    }

    @Test
    fun testAsyncBasic() {
        asyncTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        // Check for call to async
        assertTrue("Call to async was not recorded", metrics.keys.any { it.contains("Custom/Builders/async") })
        assertTrue("Async task was not recorded", metrics.keys.any { it.contains("Custom/Block/SuspendFunction/createCoroutineFromSuspendFunction") })

        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        assertEquals("Expected 1 finished transaction", 1, traces.size)

    }

    @Test
    fun testAsyncAwait() {
        awaitAllTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        // because there are 5 submits we should see 5 calls to the following functions
        val asyncMetric = metrics.get("Custom/Builders/async")
        assertNotNull(asyncMetric)
        val callCount = asyncMetric?.callCount
        assertEquals("Expected 5 async calls", 5, callCount)

        val startMetric = metrics.get("Custom/Block/SuspendFunction/createCoroutineFromSuspendFunction")
        assertNotNull(startMetric)
        val callCount2 = startMetric?.callCount
        assertEquals("Expected 5 calls to start the coroutine", 6, callCount2)

        assertTrue(
            "Should contain awaitAll metric",
            metrics.keys.any { it.contains("AwaitAll") })

        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        assertEquals("Expected 1 finished transaction", 1, traces.size)

    }

    @Test
    fun testAwaitAll() {
        awaitAllTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        // because there are 5 submits we should see 5 calls to the following functions
        val asyncMetric = metrics.get("Custom/Builders/async")
        assertNotNull(asyncMetric)
        val callCount = asyncMetric?.callCount
        assertEquals("Expected 5 async calls", 5, callCount)

        val startMetric = metrics.get("Custom/Block/SuspendFunction/createCoroutineFromSuspendFunction")
        assertNotNull(startMetric)
        val callCount2 = startMetric?.callCount
        assertEquals("Expected 5 calls to start the coroutine", 6, callCount2)

        assertTrue(
            "Should contain awaitAll metric",
            metrics.keys.any { it.contains("AwaitAll") })

        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        assertEquals("Expected 1 finished transaction", 1, traces.size)

    }

    @Test
    fun testWithContext() {
        withContextTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue(
            "Should contain withContext metric",
            metrics.keys.any { it.contains("withContext") })
    }

    /**
     * Launch multiple (3) Coroutines and use dispatcher so each executes on a different thread.
     * All three should show up as one transaction with all Coroutines linked back to the root transaction
     */
    @Test
    fun testTokenLinkingInCoroutines() {
        val latch = CountDownLatch(3)
        multipleCoroutinesTransaction(latch)
        latch.await(5, TimeUnit.SECONDS)

        val txnNames = introspector.transactionNames
        assertTrue(
            "Should have finished transactions",
            introspector.finishedTransactionCount >= 1
        )
        val txnName = txnNames.first()

        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        assertEquals("Expected 1 finished transaction", 1, traces.size)

        val coroutine1 = "Custom/Builders/launch/Coroutine1"
        val coroutine2 = "Custom/Builders/launch/Coroutine2"
        val coroutine3 = "Custom/Builders/launch/Coroutine3"

        val metrics = introspector.getMetricsForTransaction(txnName)
        assertNotNull(metrics)
        assertTrue(metrics.keys.any { it.contains(coroutine1) })
        assertTrue(metrics.keys.any { it.contains(coroutine2) })
        assertTrue(metrics.keys.any { it.contains(coroutine3) })

        // ensure that it tracked across threads as each should execute on a thread different from the main thread (1)
        val trace = traces.first()
        val initialSegment = trace.initialTraceSegment

        val coroutine1Segment = TestUtils.getRequestedTraceSegment(initialSegment, coroutine1)
        assertNotNull(coroutine1Segment)
        val attributes1 = coroutine1Segment.tracerAttributes
        val threadId1 = attributes1.get("thread.id")
        assertNotNull(threadId1)
        assertNotEquals(1, threadId1)

        val coroutine2Segment = TestUtils.getRequestedTraceSegment(initialSegment, coroutine2)
        assertNotNull(coroutine2Segment)
        val attributes2 = coroutine1Segment.tracerAttributes
        val threadId2 = attributes2.get("thread.id")
        assertNotNull(threadId2)
        assertNotEquals(1, threadId2)

        val coroutine3Segment = TestUtils.getRequestedTraceSegment(initialSegment, coroutine3)
        assertNotNull(coroutine3Segment)
        val attributes3 = coroutine1Segment.tracerAttributes
        val threadId3 = attributes3.get("thread.id")
        assertNotNull(threadId3)
        assertNotEquals(1, threadId3)

    }

    // test the flag for whether to capture a call to Delay as a segment or not.  This tests that it is captured
    @Test
    fun testDelayCaptured() {
        val utils = Utils.getInstance()
        utils.configureDelay(true)
        delayTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)
        // ensure that the Delay segment metric is included
        assertTrue(metrics.keys.contains("Custom/Delay"))
    }

    // test the flag for whether to capture a call to Delay as a segment or not.  This tests that it is captured
    @Test
    fun testDelayIgnored() {
        val utils = Utils.getInstance()
        utils.configureDelay(false)
        delayTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)
        // ensure that the Delay segment metric is not included
        assertFalse(metrics.keys.contains("Custom/Delay"))
        // set back to default true value
        utils.configureDelay(true)
    }

    @Test
    fun testYield() {
        yieldTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)

        // ensure that the yield calls show up. there should be two.
        var count = 0
        for(metric in metrics) {
            if(metric.key.startsWith("Custom/Kotlin/Yield/yield/Continuation")) {
                count++
            }
        }
        assertEquals(2, count)
    }

    @Test
    fun testChannelSendReceive() {
        channelTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)

        // ensure that there are 5 sends and 5 receives
        val sendMetricName = "Custom/Kotlin/Coroutines/Channel/BufferedChannel/send"
        val receiveMetricName = "Custom/Kotlin/Coroutines/Channel/BufferedChannel/receive"

        val sendMetric = metrics.get(sendMetricName)
        assertNotNull("Transaction metric should exist", sendMetric)
        val sendCount = sendMetric?.callCount

        assertEquals(5, sendCount)

        val receiveMetric = metrics.get(receiveMetricName)

        assertNotNull(receiveMetric)
        assertEquals(5, receiveMetric?.callCount)
    }

    @Test
    fun testChannelWithMultipleProducersConsumers() {
        multiChannelTransaction()

        assertTrue(
            "Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1
        )

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()

        val metrics = introspector.getMetricsForTransaction(txnName)
        assertNotNull("Transaction metrics should exist", metrics)

        // should see 3 calls to launch Send coroutine
        val sendCoroutineName = "Custom/Builders/launch/SendCoroutine"
        val sendCoroutine = metrics.get(sendCoroutineName)
        assertNotNull("Transaction metrics should exist", sendCoroutine)
        assertEquals(3, sendCoroutine?.callCount)

        val sendMetricName = "Custom/Kotlin/Coroutines/Channel/BufferedChannel/send"
        val sendMetric = metrics.get(sendMetricName)
        assertEquals(15, sendMetric?.callCount)

        val receiveName = "Custom/Kotlin/Coroutines/Channel/BufferedChannel/receive"
        val receiveMetric = metrics.get(receiveName)
        assertNotNull(receiveMetric)
        assertEquals(10, receiveMetric?.callCount)

        assertTrue(metrics.containsKey("Custom/Builders/launch/consumer1"))
        assertTrue(metrics.containsKey("Custom/Builders/launch/consumer2"))


        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertNotNull(traces)

    }

    @Test
    fun testExceptionInCoroutine() {
        exceptionTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())

        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)
        assertNotNull("Transaction metrics should exist", metrics)
        // Because an error is thrown, it should cause the coroutine to be canceled which is the following metric
//        assertTrue("Task was not dispatched as expected", metrics.keys.any { it.contains("Java/kotlinx.coroutines.CancellableContinuationImpl/cancel") })

    }

    /**
     * Ensure that the exception is thrown and handled
     */
    @Test
    fun testExceptionHandlerInCoroutine() {
        exceptionHandlerTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        //  check for errors and error events
        val errors = introspector.errors
        assertTrue(errors.isNotEmpty())
        assertEquals("Expected 1 error", 1, errors.size)
        val errorEvents = introspector.errorEvents
        assertEquals("Expected 1 error event", 1, errorEvents.size)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()

        val metrics = introspector.getMetricsForTransaction(txnName)
        assertNotNull("Transaction metrics should exist", metrics)

        assertTrue("Task was not dispatched as expected", metrics.keys.any { it.contains("Java/kotlinx.coroutines.CoroutineExceptionHandlerKt/handleCoroutineException") })

    }

    @Test
    fun testNestedCoroutines() {
        val latch = CountDownLatch(1)
        nestedCoroutinesTransaction(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Should have at 1 finished transaction",
            introspector.finishedTransactionCount == 1
        )

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)

        // ensure that we see the launch of each of the three coroutines
        assertTrue(metrics.keys.contains("Custom/Builders/launch/Root"))
        assertTrue(metrics.keys.contains("Custom/Builders/launch/Child"))
        assertTrue(metrics.keys.contains("Custom/Builders/launch/GrandChild"))
    }

    @Test
    fun testNestedCoroutinesWithDispatchers() {
        val latch = CountDownLatch(1)
        nestedCoroutinesTransactionWithDispatchers(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Should have at 1 finished transaction",
            introspector.finishedTransactionCount == 1
        )

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)
        val rootName = "Custom/Builders/launch/Root"
        val childName = "Custom/Builders/launch/Child"
        val grandchildName = "Custom/Builders/launch/Child"

        // ensure that we see the launch of each of the three coroutines
        assertTrue(metrics.keys.contains(rootName))
        assertTrue(metrics.keys.contains(childName))
        assertTrue(metrics.keys.contains(grandchildName))
        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        val trace = traces.first()
        assertNotNull(trace)
        val initialSegment = trace.initialTraceSegment
        assertNotNull(initialSegment)
        // Each coroutine should execute on different threads
        val rootSegment = TestUtils.getRequestedTraceSegment(initialSegment, rootName)
        assertNotNull(rootSegment)
        val rootAttributes = rootSegment.tracerAttributes
        assertNotNull(rootAttributes)
        val rootThreadId = rootAttributes.get("thread.id")
        assertNotNull(rootThreadId)
        assertNotEquals(1, rootThreadId)

        val childSegment = TestUtils.getRequestedTraceSegment(initialSegment, childName)
        val childAttributes = childSegment.tracerAttributes
        assertNotNull(childAttributes)
        val childThreadId = childAttributes.get("thread.id")
        assertNotNull(childThreadId)
        assertNotEquals(1, childThreadId)

        val grandchildSegment = TestUtils.getRequestedTraceSegment(initialSegment, grandchildName)
        val grandchildAttributes = grandchildSegment.tracerAttributes
        assertNotNull(grandchildAttributes)
        val grandchildThreadId = grandchildAttributes.get("thread.id")
        assertNotNull(grandchildThreadId)
        assertNotEquals(1, grandchildThreadId)
    }

    @Test
    fun testCoroutineCancellation() {
        cancellationTransaction()

        assertTrue(
            "Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1
        )
        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()

        // since we attempt to launch 10 tasks and then cancel before all can launch we should not see 10 launch tasks
        val metrics = introspector.getMetricsForTransaction(txnName)
        assertNotNull("Transaction metrics should exist", metrics)
        val traceMetric = metrics.get("Java/kotlinx.coroutines.EventLoopImplBase\$DelayedResumeTask/run")
        assertNotNull(traceMetric)
        assertNotEquals(10, traceMetric?.callCount)
    }

    @Test
    fun testComplexCoroutineChain() {
        complexChainTransaction()

        assertTrue(
            "Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1
        )

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain multiple coroutine operations", metrics.size > 1)

        // ensure that we capture each call (runBlocking, async, 2 launch and a withContext

        val rootBlockingName = "Custom/Builders/runBlocking/Root"
        val asyncName = "Custom/Builders/async/AsyncCoroutine"
        val delayLaunchName = "Custom/Builders/launch/DelayCoroutine"
        val withContextName = "Custom/Builders/withContext/WithContextCoroutine"
        val channelLaunchName = "Custom/Builders/launch/ChannelCoroutine"

        assertTrue(metrics.keys.contains(rootBlockingName))
        assertTrue(metrics.keys.contains(asyncName))
        assertTrue(metrics.keys.contains(delayLaunchName))
        assertTrue(metrics.keys.contains(withContextName))
        assertTrue(metrics.keys.contains(channelLaunchName))
    }

    @Test
    fun testHighConcurrency() {
        highConcurrencyTransaction()

        assertTrue(
            "Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1
        )
        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()

        // since we launched 50 components, expect to 50 launch calls
        val metrics = introspector.getMetricsForTransaction(txnName)
        val traceMetric = metrics.get("Custom/Builders/launch")
        assertNotNull(traceMetric)
        val callCount = traceMetric?.callCount
        assertEquals(50, callCount)
    }

    @Test
    fun testCustomDispatchers() {
        customDispatcherTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should track coroutines on custom dispatcher", metrics.isNotEmpty())
        /*
        When the task is dispatched, it should create an instance of NRRunnable to wrap the dispatched task.
         */
//        assertTrue("Task was not dispatched as expected", metrics.keys.any { it.contains("Custom/DispatchedTask/") })

        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        assertEquals("Expected 1 finished transaction", 1, traces.size)

        /*
        Ensure that the dispatched task is on a thread other than the main thread
         */
        val trace = traces.first()
        val initialSegment = trace.initialTraceSegment

        val requestedSegment = TestUtils.getRequestedTraceSegmentStartsWith(initialSegment, "Custom/DispatchedTask/DispatchedContinuation[java.util.concurrent.ThreadPoolExecutor")

        if (requestedSegment != null) {
            val attributes = requestedSegment.tracerAttributes
            assertNotNull(attributes)
            val threadId = attributes.get("thread.id")
            assertNotNull(threadId)
            assertNotEquals(1, threadId)
        }
    }

    /**
     * Dispatch two tasks to separate dispatchers and assert that they run on different threads
     */
    @Test
    fun testCoroutineDispatchers() {
        dispatcherTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should track coroutines on dispatcher", metrics.isNotEmpty())

        val traces = introspector.getTransactionTracesForTransaction(txnName)
        assertTrue(traces.isNotEmpty())
        assertEquals("Expected 1 finished transaction", 1, traces.size)
        /*
        Ensure that the dispatched task is on a thread other than the main thread
         */
        val trace = traces.first()
        val initialSegment = trace.initialTraceSegment

        val task1Segment = TestUtils.getRequestedTraceSegmentStartsWith(
            initialSegment,
            "Custom/DispatchedTask/DispatchedContinuation[Dispatchers.IO"
        )

        if (task1Segment != null) {
            val attributes1 = task1Segment.tracerAttributes
            assertNotNull(attributes1)
            val threadID1 = attributes1.get("thread.id")
            // assert not execution on main thread
            assertNotEquals(1, threadID1)
            val task2Segment = TestUtils.getRequestedTraceSegmentStartsWith(
                initialSegment,
                "Custom/DispatchedTask/DispatchedContinuation[Dispatchers.Default"
            )

            val attributes2 = task2Segment.tracerAttributes
            assertNotNull(attributes2)
            val threadID2 = attributes2.get("thread.id")
            // assert not execution on main thread
            assertNotEquals(1, threadID2)

            // assert tasks executed on different threads
            assertNotEquals(threadID1, threadID2)
        }
    }

    // Helper methods

    @Trace(dispatcher = true)
    fun runBlockingTransaction() {
        runBlocking {
            delay(100)
        }
    }

    @Trace(dispatcher = true)
    fun runBlockingWithName() {
        runBlocking(CoroutineName("TestName")) {
            delay(100)
        }
    }

    @Trace(dispatcher = true)
    fun launchTransaction(latch: CountDownLatch) {
        runBlocking {
            launch {
                delay(100)
                latch.countDown()
            }
        }
    }

    @Trace(dispatcher = true)
    fun launchWithName(latch: CountDownLatch) {
        runBlocking {
            launch(CoroutineName("MyLaunchCoroutine")) {
                delay(100)
                latch.countDown()
            }
        }
    }

    @Trace(dispatcher = true)
    fun asyncTransaction() {
        runBlocking {
            val deferred = async {
                task1()
                "result"
            }
            deferred.await()
        }
    }

    @Trace(dispatcher = true)
    fun asyncAwaitTransaction() {
        runBlocking {
            val deferred1 = async {
                task1()
                "result1"
            }
            val deferred2 = async {
                task2()
                "result2"
            }
            deferred1.await()
            deferred2.await()
        }
    }

    @Trace(dispatcher = true)
    fun awaitAllTransaction() {
        runBlocking {
            val deferreds = List(5) {
                async {
                    delay(50)
                    it
                }
            }
            awaitAll(*deferreds.toTypedArray())
        }
    }

    @Trace(dispatcher = true)
    fun withContextTransaction() {
        runBlocking {
            withContext(Dispatchers.IO) {
                delay(100)
            }
            withContext(Dispatchers.Default) {
                delay(100)
            }
        }
    }

    @Trace(dispatcher = true)
    fun multipleCoroutinesTransaction(latch: CountDownLatch) {
        var count = 1
        runBlocking {
            repeat(3) {
                launch(CoroutineName("Coroutine"+count) + Dispatchers.Default) {
                    delay(50)
                    latch.countDown()
                }
                count++
            }
        }
    }

    @Trace(dispatcher = true)
    fun delayTransaction() {
        runBlocking {
            delay(100)
            delay(100)
        }
    }

    @Trace(dispatcher = true)
    fun yieldTransaction() {
        runBlocking {
            yield()
            delay(50)
            yield()
        }
    }

    @Trace(dispatcher = true)
    fun channelTransaction() {
        runBlocking {
            val channel = Channel<Int>()
            launch {
                repeat(5) {
                    channel.send(it)
                }
                channel.close()
            }
            launch {
                var value = -1
                while(value < 4) {
                    value = channel.receive()

                }
            }
        }
    }

    @Trace(dispatcher = true)
    fun multiChannelTransaction() {
        runBlocking {
            val channel = Channel<Int>(15)

            repeat(3) {
                launch(CoroutineName("SendCoroutine")) {
                    repeat(5) { i ->
                        channel.send(i)
                    }
                }
            }

            val consumer1 = launch(CoroutineName("consumer1")) {
                repeat(5) {
                    channel.receive()
                }
            }
            val consumer2 = launch(CoroutineName("consumer2")) {
                repeat(5) {
                    channel.receive()
                }
            }

            consumer1.join()
            consumer2.join()
            channel.close()
        }
    }

    @Trace(dispatcher = true)
    fun exceptionTransaction() {
        try {
            runBlocking {
                launch {
                    throw RuntimeException("Test exception")
                }
                delay(200)
            }
        } catch (e: Exception) {
            // Expected exception
        }
    }

    @Trace(dispatcher = true)
    fun exceptionHandlerTransaction() {
        val handler = CoroutineExceptionHandler { _, _ ->
            // Handle exception
        }

        runBlocking {
            supervisorScope {
                val job = launch(handler) {
                    throw RuntimeException("Handled exception")
                }
                job.join()
            }
        }
    }

    @Trace(dispatcher = true)
    fun nestedCoroutinesTransaction(latch: CountDownLatch) {
        runBlocking {
            launch(CoroutineName("Root")) {
                launch(CoroutineName("Child")) {
                    launch(CoroutineName("GrandChild")) {
                        delay(50)
                        latch.countDown()
                    }
                }
            }
        }
    }

    @Trace(dispatcher = true)
    fun nestedCoroutinesTransactionWithDispatchers(latch: CountDownLatch) {
        runBlocking {
            launch(CoroutineName("Root") + Dispatchers.Default) {
                launch(CoroutineName("Child") + Dispatchers.Default) {
                    launch(CoroutineName("GrandChild") + Dispatchers.Default) {
                        delay(50)
                        latch.countDown()
                    }
                }
            }
        }
    }

    @Trace(dispatcher = true)
    fun cancellationTransaction() {
        runBlocking {
            val job = launch {
                repeat(10) {
                    delay(100)
                }
            }
            delay(150)
            job.cancel()
            job.join()
        }
    }

    @Trace(dispatcher = true)
    fun complexChainTransaction() {
        runBlocking(CoroutineName("Root")) {
            val result1 = async(CoroutineName("AsyncCoroutine")) {
                delay(100)
                "async1"
            }

            launch(CoroutineName("DelayCoroutine")) {
                delay(50)
            }

            withContext(Dispatchers.IO + CoroutineName("WithContextCoroutine")) {
                delay(100)
                "withContext1"
            }

            val channel = Channel<String>()
            launch(CoroutineName("ChannelCoroutine")) {
                channel.send(result1.await())
                channel.close()
            }

            channel.receive()
        }
    }

    @Trace(dispatcher = true)
    fun highConcurrencyTransaction() {
        runBlocking {
            val jobs = List(50) {
                launch {
                    delay(10)
                }
            }
            jobs.joinAll()
        }
    }

    @Trace(dispatcher = true)
    fun dispatcherTransaction() {

        runBlocking(CoroutineName("DispatcherTest")) {

            launch(Dispatchers.IO + CoroutineName("DispatchedTask1")) { task1() }
            launch(Dispatchers.Default + CoroutineName("DispatchedTask2")) { task2() }
        }
    }

    @Trace(dispatcher = true)
    fun customDispatcherTransaction() {
        val executor = Executors.newFixedThreadPool(2)
        val dispatcher = executor.asCoroutineDispatcher()

        runBlocking {
            withContext(dispatcher) {
                delay(100)
            }
        }

        executor.shutdown()
    }

    @Trace
    fun checkTransactionExists(hadTransaction: AtomicBoolean) {
        hadTransaction.set(AgentBridge.getAgent().getTransaction(false) != null)
    }

    suspend fun task1() {
        delay(100)
    }

    suspend fun task2() {
        delay(125)
    }

}