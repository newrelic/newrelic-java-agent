package com.newrelic.monix.api

import com.newrelic.api.agent.{NewRelic, Segment}
import monix.eval.Task


object TraceOps {

  /**
    * Creates a segment to capture metrics for a given block of code, this will call `Transaction.startSegment(String)`,
    * execute the code block, then call `Segment.end()`. This `Segment` will show up in the Transaction Breakdown
    * table, as well as the Transaction Trace page. This `Segment` will be reported in the "Custom/" metric
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
    val txn = NewRelic.getAgent.getTransaction
    val segment = txn.startSegment(segmentName)
    try {
      block
    } finally {
      segment.end()
    }
  }

  /**
    * Creates a segment to capture metrics for a value in a Monix Task
    * When run the returned Task calls `Transaction.startSegment(String)`,
    * then calls `Segment.end()`
    * This `Segment` will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This `Segment` will be reported in the "Custom/" metric
    * e.g. The code below will produce 2 segments <i>trace segment 1</i> and <i>trace segment 2</i>
    * <pre>
    * for {
    * i <- asyncTrace("trace segment 1")(Task(1))
    * j <- asyncTrace("trace segment 2")(Task(i + 1))
    * } yield j
    * </pre>
    *
    * @param segmentName Name of the `Segment` segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param block Task the segment is to capture metrics for.
    *              The block should return a `Task`
    * @return Task returned from completed asynchronous code block
    */
  def asyncTrace[A](segmentName: String)(block: Task[A]): Task[A] = for {
    segment <- startSegment(segmentName)
    res <- endSegmentOnError(block, segment)
    _ <- Task.delay(segment.end())
  } yield res


  /**
    * Creates a segment to capture metrics for a given function, this will call `Transaction.startSegment(String)`,
    * execute the function, then call `Segment.end()`. This `Segment` will show up in the Transaction Breakdown
    * table, as well as the Transaction Trace page. This `Segment` will be reported in the "Custom/" metric
    * e.g. the code below will produce a segment <i>trace map segment</i>
    * <pre>
    * Task(1)
    * .map(traceFun("trace map segment")(i => i + 1))
    * .filter(traceFun("trace filter segment")(i => i % 2 == 0))
    * </pre>
    *
    * @param segmentName Name of the `Segment` segment in APM.
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
      val txn = NewRelic.getAgent.getTransaction
      val segment = txn.startSegment(segmentName)
      try {
        f(t)
      } finally {
        segment.end()
      }
  }

  /**
    * Creates a segment to capture metrics for given asynchronous function of return type Task[A],
    * When run the returned Task calls `Transaction.startSegment(String)`,
    * executes the function,
    * then calls `Segment.end()`
    * This `Segment` will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This `Segment` will be reported in the "Custom/" metric
    * e.g. The code below will produce 1 segment <i>trace flatMap segment</i>
    * <pre>
    * Task(1).flatMap(asyncTraceFun("trace flatMap segment")(i => Task(i + 1)))
    * </pre>
    *
    * @param segmentName Name of the `Segment` segment in APM.
    *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
    *                    <p>
    *                    if null or an empty String, the agent will report "Unnamed Segment".
    * @param f           Asynchronous function segment is to capture metrics for.
    * @tparam T Input type for function segment is to capture metrics for.
    * @tparam A Type returned from completed asynchronous function
    * @return Value returned from completed asynchronous function
    */
  def asyncTraceFun[T, A](segmentName: String)(f: T => Task[A]): T => Task[A] = { t: T =>
    for {
      segment <- startSegment(segmentName)
      evaluatedFunc <- endSegmentOnError(f(t), segment)
      _ <- Task.delay(segment.end())
    } yield evaluatedFunc
  }

  /**
    * Wraps a given block of code so that a `Transaction` will be started and completed
    * before and after the code is run.
    * When this method is invoked within the context of an existing transaction this has no effect.
    * The newly created `Transaction` will complete once the code block has been executed
    * e.g. the code below will create a Transaction with a segment <i>trace map Task</i>
    * <pre>
    * txn {
    * Task(1).map(traceFun("trace map Task")(i => i + 1))
    * }
    * </pre>
    *
    * @param body Code block to be executed inside a transaction
    * @tparam A Type returned by code block
    * @return Value returned by code block
    */
  def txn[A](body: Task[A]): Task[A] = body

  private def startSegment(segmentName: String): Task[Segment] = Task.delay {
    val txn = NewRelic.getAgent.getTransaction
    txn.startSegment(segmentName)
  }

  private def endSegmentOnError[A](sync: Task[A], segment: Segment) =
    sync.onErrorHandleWith(t => {
      segment.end()
      Task.raiseError(t)
    })
}
