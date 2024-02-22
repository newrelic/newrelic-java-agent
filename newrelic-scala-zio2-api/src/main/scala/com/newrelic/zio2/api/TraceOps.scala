/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.zio2.api

import com.newrelic.api.agent.{NewRelic, Segment}
import zio._

object TraceOps {

  /**
    * Creates a segment to capture metrics for a given block of code, this will call {@link com.newrelic.api.agent.Transaction# startSegment ( String )},
    * execute the code block, then call {@link com.newrelic.api.agent.Segment# end ( )}. This {@link Segment} will show up in the Transaction Breakdown
    * table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. The code below will produce 1 segment <i>trace segment name</i>
    * <pre>
    * trace("trace segment name") {
    * val i = 1
    * val j = 2
    * i + j
    * }
    * </pre>
    *
    * @param segmentName Name of the { @link Segment} segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param block       Code block segment is to capture metrics for
    * @tparam S Type returned from executed code block
    * @return Value returned by executed code block
    */
  def trace[S](segmentName: String)(block: => S): S = {
    val txn = NewRelic.getAgent.getTransaction()
    val segment = txn.startSegment(segmentName)
    try {
      block
    } finally {
      segment.end()
    }
  }

  /**
    * Creates a segment to capture metrics for value block : ZIO[R, E, A]
    * When run the returned ZIO[R, E, A] calls {@link com.newrelic.api.agent.Transaction# startSegment (
    * String )}, executes the input code block, then calls {@link com.newrelic.api.agent.Segment# end ( )}
    * This {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. The code below will produce 2 segments <i>trace segment 1</i> and <i>trace segment 2</i>
    * <pre>
    * for {
    * i <- asyncTrace("trace segment 1")(UIO(1))
    * j <- asyncTrace("trace segment 2")(UIO(i + 1))
    * } yield j
    * </pre>
    *
    * @param segmentName Name of the { @link Segment} segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param block       ZIO[R, E, A] value the segment is to capture metrics for.
    *                    The block should return a ZIO[R, E, A]
    * @return Value returned from completed asynchronous code block
    */
  def asyncTrace[R, E, A](segmentName: String)(block: ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      segment <- startSegment(segmentName)
      b <- endSegmentOnError(block, segment)
      _ <- ZIO.succeed(segment.end())
    } yield b

  /**
    * Creates a segment to capture metrics for a given function, this will call {@link com.newrelic.api.agent.Transaction# startSegment ( String )},
    * execute the function, then call {@link com.newrelic.api.agent.Segment# end ( )}. This {@link Segment} will show up in the Transaction Breakdown
    * table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. the code below will produce a segment <i>trace map segment</i>
    * <pre>
    * UIO(1).map(traceFun("trace map segment")(i => i + 1))
    * </pre>
    *
    * @param segmentName Name of the { @link Segment} segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param f           Function segment is to capture metrics for.
    * @tparam T Input type for function segment is to capture metrics for.
    * @tparam S Type returned from executed function
    * @return Value returned from executed function
    */
  def traceFun[T, S](segmentName: String)(f: T => S): T => S = {
    t: T =>
      val txn = NewRelic.getAgent.getTransaction()
      val segment = txn.startSegment(segmentName)
      try {
        f(t)
      } finally {
        segment.end()
      }
  }

  /**
    * Creates a segment to capture metrics for given asynchronous function of return type : ZIO[R, E, A]
    * When run the returned ZIO[R, E, A] calls {@link com.newrelic.api.agent.Transaction# startSegment ( String )},
    * executes the input function,
    * then calls {@link com.newrelic.api.agent.Segment# end ( )}
    * This {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. The code below will produce 1 segment <i>trace flatMap segment</i>
    * <pre>
    * UIO(1).flatMap(asyncTraceFun("trace flatMap segment")(i => UIO(i + 1)))
    * </pre>
    *
    * @param segmentName Name of the { @link Segment} segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param f           Asynchronous function segment is to capture metrics for.
    * @tparam T Input type for function segment is to capture metrics for.
    * @tparam S Type returned from completed asynchronous function
    * @return Value returned from completed asynchronous function
    */
  def asyncTraceFun[R, E, A, T](segmentName: String)(f: T => ZIO[R, E, A]): T => ZIO[R, E, A] = {
    t: T =>
      for {
        segment <- startSegment(segmentName)
        b <- endSegmentOnError(f(t), segment)
        _ <- ZIO.succeed(segment.end())
      } yield b
  }

  /**
    * Wraps a given block of code so that a {@link com.newrelic.api.agent.Transaction} will be started and completed
    * before and after the code is run.
    * When this method is invoked within the context of an existing transaction this has no effect.
    * The newly created {@link com.newrelic.api.agent.Transaction} will complete once the code block has been executed
    * e.g. the code below will create a Transaction and with a segment <i>trace map UIO</i>
    * <pre>
    * txn {
    * UIO(1).map(traceFun("trace map UIO")(i => i + 1))
    * }
    * </pre>
    *
    * @param block Code block to be executed inside a transaction
    * @tparam S Type returned by code block
    * @return Value returned by code block
    */
  def txn[R, E, A](body: ZIO[R, E, A]): ZIO[R, E, A] = body

  private def startSegment(segmentName: String): UIO[Segment] = ZIO.succeed {
    val txn = NewRelic.getAgent.getTransaction()
    txn.startSegment(segmentName)
  }

  private def endSegmentOnError[R, E, A](zio: ZIO[R, E, A], segment: Segment): ZIO[R, E, A] = {
    zio.catchAll(e => {
      segment.end()
      ZIO.fail(e)
    })
  }
}
