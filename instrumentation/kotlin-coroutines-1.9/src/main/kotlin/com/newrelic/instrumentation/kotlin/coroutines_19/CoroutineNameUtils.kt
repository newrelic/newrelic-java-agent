package com.newrelic.instrumentation.kotlin.coroutines_19

import kotlinx.coroutines.CoroutineName
import kotlin.coroutines.CoroutineContext

fun CoroutineContext.getCoroutineName(): String? = this[CoroutineName.Key]?.name
