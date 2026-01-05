package coroutines

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.runner.RunWith
import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test

val random: Random = Random()

@RunWith(InstrumentationTestRunner::class)
@InstrumentationTestConfig(includePrefixes = ["kotlinx.coroutines"])
class CoroutinesTest {

    @Test
    fun testCoroutine() {
        val iterations = 10
        iterations(iterations)
    }

    val async: Async = Async()
    //val undispatched: Undispatched = Undispatched()

    fun iterations(n : Int) {
        for (i in 1..n) {
            println("iteration $i of $n")
            iteration()
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun iteration() {
        async.main()
        pause(500)
    }
}