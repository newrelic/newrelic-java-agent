/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala

import com.newrelic.agent.introspec.{Introspector, TraceSegment, TransactionTrace}
import org.junit.Assert

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._

object TracerSegmentUtils {

  def checkChildrenTraces(children: java.util.List[TraceSegment], futures: List[String], callbacks: List[String], custom: List[String]) {
    // First check trace segments for scala Futures
    val futuresIter: Iterator[String] = futures.iterator
    while (futuresIter.hasNext) {
      Assert.assertTrue("Transaction missing trace segments", children.size > 0)
      val traceNameExp: String = futuresIter.next
      val idx = indexOf(traceNameExp, children)
      Assert.assertNotEquals("Missing trace segment for " + traceNameExp, -1, idx)
      children.remove(idx)
    }

    //    // Second, check trace segments for scala Future callbacks
    val callbacksIter: Iterator[String] = callbacks.iterator
    while (callbacksIter.hasNext) {
      Assert.assertTrue("Transaction is missing a trace segment", children.size > 0)
      val traceNameExp: String = callbacksIter.next
      val idx = indexOf(traceNameExp, children)
      Assert.assertNotEquals("Missing trace segment for " + traceNameExp, -1, idx)
      children.remove(idx)
    }

    // Finally, check any custom trace segments
    val customIter: Iterator[String] = custom.iterator
    while (customIter.hasNext) {
      Assert.assertTrue("Transaction is missing a trace segment", children.size > 0)
      val traceNameExp: String = customIter.next
      val idx = indexOf(traceNameExp, children)
      Assert.assertNotEquals("Missing trace segment for " + traceNameExp, -1, idx)
      children.remove(idx)
    }

    // Make sure there is no remaining unchecked trace segments
    Assert.assertTrue("Test fail to check the following trace segments: " + children, children.size == 0)
  }

  // check if the children trace segment list contains a given traceName
  // returns: the index in the children list where traceName occurs
  //          -1 if not found
  def indexOf(traceName: String, children: java.util.List[TraceSegment]): Int = {
    val childrenIter: java.util.Iterator[TraceSegment] = children.iterator();
    var res: Int = -1;
    var idx: Int = 0;
    var done = false
    while (!done && childrenIter.hasNext()) {
      val traceSegment: TraceSegment = childrenIter.next();
      if (traceSegment.getName.startsWith(traceName)) {
        done = true
        res = idx
      }
      idx = idx + 1
    }
    res
  }

  def getTraces()(implicit introspector: Introspector): Iterable[TransactionTrace] =
    introspector.getTransactionNames.asScala.flatMap(transactionName => introspector.getTransactionTracesForTransaction(transactionName).asScala)

  def getSegments(traces : Iterable[TransactionTrace]): Iterable[TraceSegment] =
    traces.flatMap(trace => this.getSegments(trace.getInitialTraceSegment))

  private def getSegments(segment: TraceSegment): List[TraceSegment] = {
    val childSegments = segment.getChildren.asScala.flatMap(childSegment => getSegments(childSegment)).toList
    segment :: childSegments
  }
}
