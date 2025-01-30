package com.newrelic.weave.weavepackage.testclasses

import kotlinx.coroutines.*

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

suspend fun doWorld() {
    println("World!")
}

fun doOtherWorld() {
    print("Non-suspending world!")
}

