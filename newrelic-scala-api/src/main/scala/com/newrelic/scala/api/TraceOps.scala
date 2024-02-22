package com.newrelic.scala.api


import com.newrelic.api.agent.{NewRelic, Trace, Transaction}

import scala.concurrent.{ExecutionContext, Future}

object TraceOps {

  /**
   * Creates a segment to capture metrics for a given block of code, this will call {@link com.newrelic.api.agent.Transaction#startSegment(String)},
   * execute the code block, then call {@link com.newrelic.api.agent.Segment#end()}. This {@link Segment} will show up in the Transaction Breakdown
   * table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
   * e.g. The code below will produce 1 segment <i>trace segment name</i>
   * <pre>
   * trace("trace segment name") {
   *    val i = 1
   *    val j = 2
   *    i + j
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
   * Creates a segment to capture metrics for a given asynchronous block of code, this will call {@link com.newrelic.api.agent.Transaction#startSegment(String)},
   * execute the code block, then call {@link com.newrelic.api.agent.Segment#end()} on the completion of the asynchronous code block.
   * This {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
   * e.g. The code below will produce 2 segments <i>trace segment 1</i> and <i>trace segment 2</i>
   * <pre>
   * for {
   *    i <- asyncTrace("trace segment 1")(Future(1))
   *    j <- asyncTrace("trace segment 2")(Future(i + 1))
   * } yield j
   * </pre>
   *
   * @param segmentName Name of the { @link Segment} segment in APM.
   *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
   *                    <p>
   *                    if null or an empty String, the agent will report "Unnamed Segment".
   * @param block       Asynchronous code block segment is to capture metrics for.
   *                    The block should return a { @link Future}
   * @param ec The execution context on which the future is run
   * @tparam S Type returned from completed asynchronous code block
   * @return Value returned from completed asynchronous code block
   */
  def asyncTrace[S](segmentName: String)(block: => Future[S])(implicit ec: ExecutionContext): Future[S] = {
    val txn = NewRelic.getAgent.getTransaction()
    val segment = txn.startSegment(segmentName)
    val evaluatedBlock = block
    evaluatedBlock.onComplete {
      case _ => segment.end()
    }
    evaluatedBlock
  }

  /**
   * Creates a segment to capture metrics for a given function, this will call {@link com.newrelic.api.agent.Transaction#startSegment(String)},
   * execute the function, then call {@link com.newrelic.api.agent.Segment#end()}. This {@link Segment} will show up in the Transaction Breakdown
   * table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
   * e.g. the code below will produce 2 segments <i>trace map segment</i> and <i>trace filter segment</i>
   * <pre>
   * Future(1)
   *    .map(traceFun("trace map segment")(i => i + 1))
   *    .filter(traceFun("trace filter segment")(i => i % 2 == 0))
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
    (t: T) =>
      val txn = NewRelic.getAgent.getTransaction()
      val segment = txn.startSegment(segmentName)
      try {
        f(t)
      } finally {
        segment.end()
      }
  }

  /**
   * Creates a segment to capture metrics for a given asynchronous function, this will call {@link com.newrelic.api.agent.Transaction#startSegment(String)},
   * execute the function, then call {@link com.newrelic.api.agent.Segment#end()} on the <b>completion</b> of the asynchronous function.
   * This {@link Segment} will show up in the Transaction Breakdown table, as well as the Transaction Trace page. This {@link Segment} will be reported in the "Custom/" metric
   * e.g. The code below will produce 1 segment <i>trace flatMap segment</i>
   * <pre>
   * Future(1).flatMap(asyncTraceFun("trace flatMap segment")(i => Future(i + 1)))
   * </pre>
   *
   * @param segmentName Name of the { @link Segment} segment in APM.
   *                    This name will show up in the Transaction Breakdown table, as well as the Transaction Trace page.
   *                    <p>
   *                    if null or an empty String, the agent will report "Unnamed Segment".
   * @param f Asynchronous function segment is to capture metrics for.
   * @param ec The execution context on which the future is run
   * @tparam T Input type for function segment is to capture metrics for.
   * @tparam S Type returned from completed asynchronous function
   * @return Value returned from completed asynchronous function
   */
  def asyncTraceFun[T, S](segmentName: String)(f: T => Future[S])(implicit ec: ExecutionContext): T => Future[S] = {
    (t: T) =>
      val txn = NewRelic.getAgent.getTransaction()
      val segment = txn.startSegment(segmentName)
      val evaluatedFunc = f(t)
      evaluatedFunc.onComplete {
        case _ => segment.end()
      }
      evaluatedFunc

  }

  /**
   * Starts a {@link com.newrelic.api.agent.Transaction} for a given block of code.
   * When this method is invoked within the context of an existing transaction this has no effect.
   * The newly created {@link com.newrelic.api.agent.Transaction} will complete once the code block has been executed
   * e.g. the code below will create a Transaction and with 2 segments <i>trace option creation</i> and <i>trace map option</i>
   * <pre>
   * txn {
   *    val o1 = trace("trace option creation")(Some(1))
   *    o1.map(traceFun("trace map option")(i => i + 1))
   * }
   * </pre>
   * e.g. the code below will create a Transaction and with 2 segments <i>trace map future</i> and <i>trace filter future</i>
   * <pre>
   * txn {
   *    Future(1)
   *        .map(traceFun("trace map future")(i => i + 1))
   *        .filter(traceFun("trace filter future")(i => i % 2 == 0))
   * }
   * </pre>
   *
   * @param block Code block to be executed inside a transaction
   * @tparam S Type returned by code block
   * @return Value returned by code block
   */
  @Trace(dispatcher = true)
  def txn[S](block: => S): S = block

}

