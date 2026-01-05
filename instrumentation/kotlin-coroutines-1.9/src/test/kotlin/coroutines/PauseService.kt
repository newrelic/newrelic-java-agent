package coroutines

import kotlinx.coroutines.*

fun pause(ms: Long) {
        if(ms > 0L) {
            runBlocking {
                pauseActual(ms)
            }
        }
}

suspend fun pauseActual(ms: Long) {
    delay(ms)
}