package com.newrelic.instrumentation.kotlin.suspends

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

fun getCoroutineExceptionHandler(context: CoroutineContext): CoroutineExceptionHandler? = context.get(CoroutineExceptionHandler.Key)