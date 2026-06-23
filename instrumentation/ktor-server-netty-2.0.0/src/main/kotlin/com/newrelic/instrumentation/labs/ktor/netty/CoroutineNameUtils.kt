/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.instrumentation.labs.ktor.netty

import kotlinx.coroutines.CoroutineName
import kotlin.coroutines.CoroutineContext

fun CoroutineContext.getCoroutineName(): String? = this[CoroutineName.Key]?.name
