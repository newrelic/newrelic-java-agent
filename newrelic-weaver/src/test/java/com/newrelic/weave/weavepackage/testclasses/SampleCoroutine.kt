/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


fun doOneSuspend() = runBlocking{
    launch {
        doWorld();
    }
}

fun doThreeSuspends() = runBlocking {
    launch {
        doWorld();
        doWorld();
        doWorld();
    }
}

fun doNoSuspends() = runBlocking {
    launch {
        doOtherWorld();
    }
}

fun doNestedSuspends() = runBlocking{
    launch {
        nested()
    }
}
suspend fun doWorld() {
    println("World!")
}

fun doOtherWorld() {
    print("Non-suspending world!")
}

suspend fun nested(){
    delay(1000L)
    doWorld()
}

