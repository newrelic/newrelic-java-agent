package com.newrelic.cats.api

import cats.effect.Sync
import cats.implicits._
import com.newrelic.api.agent.{NewRelic, Segment}

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
    * Creates a segment to capture metrics for a value in a context F[A] where there is a
    * cats effect Sync Type Class instance for the context F, this will typically be cats effect IO
    * When run the returned F[S] calls {@link com.newrelic.api.agent.Transaction# startSegment ( String )},
    * executes the input F[S], then calls {@link com.newrelic.api.agent.Segment# end ( )}
    * This {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. The code below will produce 2 segments <i>trace segment 1</i> and <i>trace segment 2</i>
    * <pre>
    * for {
    * i <- asyncTrace("trace segment 1")(IO(1))
    * j <- asyncTrace("trace segment 2")(IO(i + 1))
    * } yield j
    * </pre>
    *
    * @param segmentName Name of the { @link Segment} segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param block       F[S] value the segment is to capture metrics for.
    *                    The block should return a { @link IO}
    * @return Value returned from completed asynchronous code block
    */
  def asyncTrace[S, F[_] : Sync](segmentName: String)(block: F[S]): F[S] = for {
    segment <- startSegment(segmentName)
    res <- endSegmentOnError(block, segment)
    _ <- Sync[F].delay(segment.end())
  } yield res


  /**
    * Creates a segment to capture metrics for a given function, this will call {@link com.newrelic.api.agent.Transaction# startSegment ( String )},
    * execute the function, then call {@link com.newrelic.api.agent.Segment# end ( )}. This {@link Segment} will show up in the Transaction Breakdown
    * table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. the code below will produce a segment <i>trace map segment</i>
    * <pre>
    * IO(1)
    * .map(traceFun("trace map segment")(i => i + 1))
    * .filter(traceFun("trace filter segment")(i => i % 2 == 0))
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
    * Creates a segment to capture metrics for given asynchronous function of return type :F[S],
    * When run the returned F[S] calls {@link com.newrelic.api.agent.Transaction# startSegment ( String )},
    * executes the function,
    * then call {@link com.newrelic.api.agent.Segment# end ( )}
    * This {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
    * e.g. The code below will produce 1 segment <i>trace flatMap segment</i>
    * <pre>
    * IO(1).flatMap(asyncTraceFun("trace flatMap segment")(i => IO(i + 1)))
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
  def asyncTraceFun[T, S, F[_] : Sync](segmentName: String)(f: T => F[S]): T => F[S] = { t: T =>
    for {
      segment <- startSegment(segmentName)
      evaluatedFunc <- endSegmentOnError(f(t), segment)
      _ <- Sync[F].delay(segment.end())
    } yield evaluatedFunc
  }

  /**
    * Wraps a given block of code so that a {@link com.newrelic.api.agent.Transaction} will be started and completed
    * before and after the code is run.
    * When this method is invoked within the context of an existing transaction this has no effect.
    * The newly created {@link com.newrelic.api.agent.Transaction} will complete once the code block has been executed
    * e.g. the code below will create a Transaction and with 2 segments <i>trace map IO</i> and <i>trace filter IO</i>
    * <pre>
    * txn {
    * IO(1).map(traceFun("trace map IO")(i => i + 1))
    * }
    * </pre>
    *
    * @param block Code block to be executed inside a transaction
    * @tparam S Type returned by code block
    * @return Value returned by code block
    */
  def txn[S, F[_] : Sync](body: F[S]): F[S] = body

  private def startSegment[F[_] : Sync](segmentName: String): F[Segment] = Sync[F].delay {
    val txn = NewRelic.getAgent.getTransaction()
    txn.startSegment(segmentName)
  }

  private def endSegmentOnError[S, F[_] : Sync](io: F[S], segment: Segment) =
    io.handleErrorWith(t => {
      segment.end()
      Sync[F].raiseError(t)
    })
}
