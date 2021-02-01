/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala

import java.util

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.agent.introspec._
import com.newrelic.api.agent.Trace
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala.concurrent.impl."))
class ProducerConsumerWithFutures {
  def produceSomething(f: Future[Int]): Int = {
    val r = scala.util.Random
    var result = r.nextInt(100)
    println("Producer  produced " + result + " to be sent to " + f)
    result
  }

  def startDoingSomething(): Unit = {
    println("Consumer sleeping for 100ms")
    Thread.sleep(scala.util.Random.nextInt(100))
  }

  @Trace(dispatcher = true)
  def producerConsumerTx() {
    for (i <- 1 to 5) {
      val p = Promise[Int]
      val f = p.future
      val producer = Future {
        val res = produceSomething(f)
        p success res
        println("Producer sleeping for 100ms")
        Thread.sleep(scala.util.Random.nextInt(200))
      }
      val consumer = Future {
        startDoingSomething()
        f.onComplete(res => println("Consumer " + f + " consumed " + res))
      }
    }
  }

  @Test
  def testProducerConsumer(): Unit = {
    producerConsumerTx()
    Thread.sleep(1000)
    val introspector: Introspector = InstrumentationTestRunner.getIntrospector
    Assert.assertEquals(1, introspector.getFinishedTransactionCount(30000))

    var asyncTransaction: String = null
    println("TransactionNames: " + introspector.getTransactionNames)
    val it: java.util.Iterator[String] = introspector.getTransactionNames.iterator()
    while (it.hasNext) {
      val txName: String = it.next()
      println("txName: " + txName)
      if (txName.matches(".*ProducerConsumerWithFutures.*")) {
        asyncTransaction = txName
      }
    }
    Assert.assertNotNull("Unable to find test transaction", asyncTransaction)
    val names: util.Collection[String] = introspector.getTransactionNames
    val txName: String = new String("OtherTransaction/Custom/" + this.getClass.getName + "/producerConsumerTx")
    Assert.assertTrue("Unable to find transaction " + txName, names.contains(txName))

    val transactionTraces: util.Collection[TransactionTrace] = introspector.getTransactionTracesForTransaction(txName)
    val ttIter: java.util.Iterator[TransactionTrace] = transactionTraces.iterator()
    while (ttIter.hasNext) {
      val txTrace: TransactionTrace = ttIter.next()
      println("Tracer name: " + txTrace.getInitialTraceSegment.getName)
      val traceSegIter: java.util.Iterator[TraceSegment] = txTrace.getInitialTraceSegment.getChildren.iterator()
      while (traceSegIter.hasNext) {
        System.out.println("trace segment name: " + traceSegIter.next().getName)
      }
      TracerSegmentUtils.checkChildrenTraces(txTrace.getInitialTraceSegment.getChildren,
        List(),
        List(),
        List())
    }
  }
}