/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala

import java.util.Collection
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.agent.introspec._
import com.newrelic.api.agent.Trace
import com.newrelic.test.marker.Java17IncompatibleTest
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, _}
import scala.util.{Failure, Random, Success};

@Category(Array(classOf[Java17IncompatibleTest]))
@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("scala.concurrent.impl."))
class ScalaFutureTest {

  @Test
  def testFuture() { 
    backToTheFutureTx();
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 1);
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())

    var asyncTransaction :String = null;
    val it :java.util.Iterator[String] = introspector.getTransactionNames().iterator()
    while(it.hasNext()) {
      val txName :String = it.next();
      if (txName.matches(".*ScalaFutureTest.*")) {
        asyncTransaction = txName;
      }
    }
    
    val names : Collection[String] = introspector.getTransactionNames();
    val txName : String = new String("OtherTransaction/Custom/" + this.getClass().getName() +  "/backToTheFutureTx");
    Assert.assertTrue("Unable to find transaction " + txName , names.contains(txName));
    Assert.assertNotNull("Unable to find transaction", asyncTransaction);

    // Events
    val transactionEvents :Collection[TransactionEvent] = introspector.getTransactionEvents(asyncTransaction);
    Assert.assertEquals(1, transactionEvents.size());

    // Check transaction traces
    val transactionTraces :Collection[TransactionTrace] = introspector.getTransactionTracesForTransaction(txName)
    val ttIter :java.util.Iterator[TransactionTrace] = transactionTraces.iterator()
    val rootTracer = "Java/" + this.getClass().getName + "/backToTheFutureTx"
    while(ttIter.hasNext()) {
      val txTrace :TransactionTrace = ttIter.next();
      Assert.assertEquals(rootTracer, txTrace.getInitialTraceSegment.getName())
      TracerSegmentUtils.checkChildrenTraces(txTrace.getInitialTraceSegment().getChildren(),
                    List(),
                    List(),
                    List("Custom/com.nr.agent.instrumentation.scala.ScalaFutureTest/traveling",
                         "Custom/com.nr.agent.instrumentation.scala.ScalaFutureTest/traveling"));
    }
  }

  @Trace(dispatcher = true)
  def backToTheFutureTx() {
    // This future will always produce (send you to) TimeMachine.NOV051955, but more importantly, when?
    val future1955 = Future {
      sleep(Random.nextInt(100))
      println("Traveling back to NOV/05/1955")
      TimeMachine.NOV051955
    }
 
    Thread.sleep(1000)
    traveling();

    // Register first callback: make sure future1955 is producing (sending you back to) TimeMachine.NOV051955 
    future1955.onComplete {
      case Success(value) => Assert.assertEquals(TimeMachine.NOV051955, value)
      case Failure(e) => e.printStackTrace
    }
    traveling();
    
    Thread.sleep(100)

    var year :Int = TimeMachine.NOV051955;
    // Register a second callback: which will change the year back to (the future) TimeMachine.OCT261985
    future1955.onComplete {
      // this call back will change the year to (send you back to) TimeMachine.OCT261985
      case Success(value) => year = (value - 789970)
      case Failure(e) =>  year = 0
    }

    Thread.sleep(100)
    val date1955 = Await.result(future1955, 4 seconds)
    // check the results of both callbacks
    Assert.assertEquals(TimeMachine.OCT261985, year)
    Assert.assertEquals(TimeMachine.NOV051955, date1955)
  }

  @Trace
  def traveling() {
    println ("I am either traveling to/at the past")
  }

  @Test
  def testFallback() {
    backToTheFuture2Tx();
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 1);
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())

    var asyncTransaction :String = null;
    val it :java.util.Iterator[String] = introspector.getTransactionNames().iterator()
    while(it.hasNext()) {
      val txName :String = it.next();
      if (txName.matches(".*ScalaFutureTest.*")) {
        asyncTransaction = txName;
      }
    }

    Assert.assertNotNull("Unable to find a transaction", asyncTransaction);
    val names : Collection[String] = introspector.getTransactionNames();
    val txName : String = new String("OtherTransaction/Custom/" + this.getClass().getName() +  "/backToTheFuture2Tx");
    Assert.assertTrue("Unable to find transaction " + txName , names.contains(txName));

    val transactionTraces :Collection[TransactionTrace] = introspector.getTransactionTracesForTransaction(txName)
    val ttIter :java.util.Iterator[TransactionTrace] = transactionTraces.iterator()
    val rootTracer = "Java/" + this.getClass().getName + "/backToTheFuture2Tx"
    while(ttIter.hasNext()) {
      val txTrace :TransactionTrace = ttIter.next();
      Assert.assertEquals(rootTracer, txTrace.getInitialTraceSegment.getName);
      TracerSegmentUtils.checkChildrenTraces(txTrace.getInitialTraceSegment().getChildren(),
                    List(),
                    List(),
                    List());
    }
  }

  @Trace(dispatcher = true)
  def backToTheFuture2Tx() {
   // Assume we are already in the future TimeMachine.OCT212015 (the future is already the past :( )
   // Something wrong will happen in future2015, and it will fallback (send you back) to future1955
   val future2015 = Future {
      Thread.sleep(200)
      TimeMachine.NOV121955 / 0
    }

    // This is the fallback future future1955
    val future1955 = Future {
      Thread.sleep(100)
      TimeMachine.OCT261985
    }
    Thread.sleep(300)

    // future1955 is a fallback (i.e. a callback) of future2015 (onFailure)
    // If something wrong happens in future2015, as it will, the execution
    // flow will revert back to future1955
    future2015 fallbackTo future1955 onSuccess {
      case v => Assert.assertEquals("Fallback failed", 10261985, v)
    }

    // Register a callback that will never execute, since future2015 throws an exception
    future2015 onSuccess {
      case x => Assert.assertTrue("Should never return back from future2015 onSuccess", false)
    }

    // Register another call back, to make sure future2015 is always throwing an exception and taking the
    // execution flow back to future1955 (past)
    future2015 onFailure {
      case e => Assert.assertTrue("future2015 should have thrown an ArithmeticException but trowed " + e, e.isInstanceOf[ArithmeticException])
    }

    Thread.sleep(300)

    // make sure future1955 is taking the execution back to the future (TimeMachine.OCT261985)
    future1955.onComplete {
      // Check if future1955 correctly sent the execution flow back to the Future
      case Success(x) => Assert.assertEquals(TimeMachine.OCT261985, x)
      case Failure(e) => Assert.assertTrue("future1955 should send you back to the future, and never fail with " + e, false)
    }
    Thread.sleep(3000)
  }

  def sleep(duration: Long) { Thread.sleep(duration) } 

  // introspector does not handle async tx finishing very well so we're sleeping as a workaround
  private def awaitFinishedTx(introspector :Introspector, expectedTxCount: Int = 1) {
    while(introspector.getFinishedTransactionCount() <= expectedTxCount-1) {
      Thread.sleep(100)
    }
    Thread.sleep(100)
  }

  @Test
  def testErrorInFuture() {
    for (i <- 0 to 9) {
      errorInFutureTest()
      Assert.assertTrue(nonTracedFuture())
    }

    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    Assert.assertEquals(10, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(10)))
  }

  @Trace(dispatcher = true)
  def errorInFutureTest(): Unit = {
    val f = Future {
      // this future will throw an exception and cause the state to not be reset correctly (if our try/finally doesn't work)
      throw new InterruptedException("Boom")
    }
    f.onComplete(value => value.get)
  }
  
  def nonTracedFuture(): Boolean = {
    val transactionIsNull = new AtomicBoolean(false)
    var f = Future {
      transactionIsNull.set(AgentBridge.getAgent.getTransaction(false) == null)
    }
    Await.result(f, 5 seconds)
    transactionIsNull.get()
  }
  
}

// Define constants for the TimeMachine
object TimeMachine {
    val NOV051955 :Int = 11051955;
    val NOV121955 :Int = 11121955;
    val OCT261985 :Int = 10261985;
    val OCT212015 :Int = 10212015;
}
