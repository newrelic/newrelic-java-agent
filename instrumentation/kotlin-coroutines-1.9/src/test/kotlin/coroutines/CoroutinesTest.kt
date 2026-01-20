package coroutines

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector
import com.newrelic.api.agent.Trace
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
        assertTrue("Should contain runBlocking metric",
            metrics.keys.any { it.contains("runBlocking") || it.contains("Builders") })
    }

    @Test
    fun testRunBlockingWithCoroutineName() {
        runBlockingWithName()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain runBlocking metric with name",
            metrics.keys.any { it.contains("runBlocking") && it.contains("TestName") })
    }

    @Test
    fun testLaunchBasic() {
        val latch = CountDownLatch(1)
        launchTransaction(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain launch metric",
            metrics.keys.any { it.contains("launch") || it.contains("Builders") })
    }

    @Test
    fun testLaunchWithCoroutineName() {
        val latch = CountDownLatch(1)
        launchWithName(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain launch metric with coroutine name",
            metrics.keys.any { it.contains("launch") || it.contains("MyLaunchCoroutine") || it.contains("Builders") })
    }

    @Test
    fun testAsyncBasic() {
        asyncTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain async metric",
            metrics.keys.any { it.contains("async") })
    }

    @Test
    fun testAsyncAwait() {
        asyncAwaitTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain async metric",
            metrics.keys.any { it.contains("async") })
    }

    @Test
    fun testAwaitAll() {
        awaitAllTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain awaitAll or await metric",
            metrics.keys.any { it.contains("await") || it.contains("AwaitAll") })
    }

    @Test
    fun testWithContext() {
        withContextTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain withContext metric",
            metrics.keys.any { it.contains("withContext") })
    }

    @Test
    fun testTokenPropagationAcrossThreads() {
        val hadTransaction = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        tokenPropagationTransaction(hadTransaction, latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should have transaction in async context", hadTransaction.get())
        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)
    }

    @Test
    fun testTokenLinkingInCoroutines() {
        val latch = CountDownLatch(3)
        multipleCoroutinesTransaction(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should have finished transactions",
            introspector.finishedTransactionCount >= 1)
    }

    @Test
    fun testDelay() {
        delayTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)
    }

    @Test
    fun testYield() {
        yieldTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)
    }

    @Test
    fun testChannelSendReceive() {
        channelTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertNotNull("Transaction metrics should exist", metrics)
    }

    @Test
    fun testChannelWithMultipleProducersConsumers() {
        multiChannelTransaction()

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)
    }

    @Test
    fun testExceptionInCoroutine() {
        exceptionTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
    }

    @Test
    fun testExceptionHandlerInCoroutine() {
        exceptionHandlerTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)
    }

    @Test
    fun testNestedCoroutines() {
        val latch = CountDownLatch(1)
        nestedCoroutinesTransaction(latch)
        latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)
    }

    @Test
    fun testCoroutineCancellation() {
        cancellationTransaction()

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)
    }

    @Test
    fun testComplexCoroutineChain() {
        complexChainTransaction()

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)

        val txnNames = introspector.transactionNames
        assertFalse("Should have transaction names", txnNames.isEmpty())
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should contain multiple coroutine operations", metrics.size > 1)
    }

    @Test
    fun testHighConcurrency() {
        highConcurrencyTransaction()

        assertTrue("Should have at least 1 finished transaction",
            introspector.finishedTransactionCount >= 1)
    }

    @Test
    fun testCustomDispatchers() {
        customDispatcherTransaction()

        assertEquals("Expected 1 finished transaction", 1, introspector.finishedTransactionCount)

        val txnNames = introspector.transactionNames
        val txnName = txnNames.first()
        val metrics = introspector.getMetricsForTransaction(txnName)

        assertTrue("Should track coroutines on custom dispatcher", metrics.isNotEmpty())
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
                delay(100)
                "result"
            }
            deferred.await()
        }
    }

    @Trace(dispatcher = true)
    fun asyncAwaitTransaction() {
        runBlocking {
            val deferred1 = async {
                delay(100)
                "result1"
            }
            val deferred2 = async {
                delay(100)
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
    fun tokenPropagationTransaction(hadTransaction: AtomicBoolean, latch: CountDownLatch) {
        runBlocking {
            launch(Dispatchers.Default) {
                checkTransactionExists(hadTransaction)
                latch.countDown()
            }
        }
    }

    @Trace(dispatcher = true)
    fun multipleCoroutinesTransaction(latch: CountDownLatch) {
        runBlocking {
            repeat(3) {
                launch {
                    delay(50)
                    latch.countDown()
                }
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
                for (value in channel) {
                    // Consume values
                }
            }
        }
    }

    @Trace(dispatcher = true)
    fun multiChannelTransaction() {
        runBlocking {
            val channel = Channel<Int>(10)

            repeat(3) {
                launch {
                    repeat(5) { i ->
                        channel.send(i)
                    }
                }
            }

            val consumer1 = launch {
                repeat(5) {
                    channel.receive()
                }
            }
            val consumer2 = launch {
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
            launch {
                launch {
                    launch {
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
        runBlocking {
            val result1 = async {
                delay(100)
                "async1"
            }

            launch {
                delay(50)
            }

            withContext(Dispatchers.IO) {
                delay(100)
                "withContext1"
            }

            val channel = Channel<String>()
            launch {
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
}