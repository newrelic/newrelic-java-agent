package com.newrelic.zio.api

import com.newrelic.api.agent.{NewRelic, Segment}
import zio._

object TraceOps {

  def trace[S](segmentName: String)(block: => S): S = {
    val txn = NewRelic.getAgent.getTransaction()
    val segment = txn.startSegment(segmentName)
    try {
      block
    } finally {
      segment.end()
    }
  }

  def asyncTrace[R, E, A](segmentName: String)(block: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      segment <- startSegment(segmentName)
      b <- endSegmentOnError(block, segment)
      _ <- ZIO.succeed(segment.end())
    } yield b

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

  def asyncTraceFun[R, E, A, T](segmentName: String)(f: T => ZIO[R, E, A]): T => ZIO[R, E, A] = {
    t: T =>
      for {
        segment <- startSegment(segmentName)
        b <- endSegmentOnError(f(t), segment)
        _ <- ZIO.succeed(segment.end())
      } yield b
  }

  def txn[R, E, A](body: ZIO[R, E, A]): ZIO[R, E, A] = body

  private def startSegment(segmentName: String): UIO[Segment] = UIO {
    val txn = NewRelic.getAgent.getTransaction()
    txn.startSegment(segmentName)
  }

  private def endSegmentOnError[R, E, A](zio: ZIO[R, E, A], segment: Segment): ZIO[R, E, A] =
    zio.catchAll(e => {
      segment.end()
      ZIO.fail(e)
    })
}
