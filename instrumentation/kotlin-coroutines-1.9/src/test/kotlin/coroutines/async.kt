package coroutines

import com.newrelic.api.agent.Trace
import kotlinx.coroutines.*
import java.util.concurrent.Executors

// this launches 2 coroutines and uses async() and await() to later wait for the results

class Async {

    @Trace(dispatcher = true)
    fun main() {
        suspend fun task1(): String {
            println("start task1 in Thread ${java.lang.Thread.currentThread()}")
            yield()
            delay(2000)
            println("end task1 in Thread ${java.lang.Thread.currentThread()}")

            return "foobar"
        }

        suspend fun task2() {
            println("start task2 in Thread ${java.lang.Thread.currentThread()}")
            yield()
            println("end task2 in Thread ${java.lang.Thread.currentThread()}")
        }

        println("start")

        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { context ->
            runBlocking(CoroutineName("Async1")) {
                val results = mutableListOf<Deferred<String>>()

                repeat(10) {
                    results.add(async(context) {
                        println("in iteration $it, for async task 1 context is $context")
                        task1()
                    })
                }

                launch { task2() }

                println("called task1 and task2 from ${java.lang.Thread.currentThread()}")

                // now wait for the results
                println(results.joinToString(", ") { runBlocking { it.await() } })
            }
        }

        println("done")
    }
}
