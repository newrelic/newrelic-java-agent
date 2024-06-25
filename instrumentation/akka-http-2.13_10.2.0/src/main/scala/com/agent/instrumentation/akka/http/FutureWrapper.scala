package com.agent.instrumentation.akka.http

import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.weaver.Weaver

import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.Try

class FutureWrapper[T](val original: Future[T]) extends Future[T] {

  override def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
    try {
      val wrappedFunction: Try[T] => U = new Function1Wrapper(f, NewRelic.getAgent.getTransaction.getToken)
      original.onComplete(wrappedFunction)
    } catch {
      case t: Throwable => {
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        original.onComplete(f)
      }
    }
  }

  override def isCompleted: Boolean = original.isCompleted

  override def value: Option[Try[T]] = original.value

  override def failed: Future[Throwable] = original.failed

  override def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Future[S] = original.transform(f)

  override def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): Future[S] = original.transformWith(f)

  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    original.ready(atMost)
    this
  }

  override def result(atMost: Duration)(implicit permit: CanAwait): T = original.result(atMost)

  override def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = original.foreach(f)

  override def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext): Future[S] = original.transform(s, f)

  override def map[S](f: T => S)(implicit executor: ExecutionContext): Future[S] = original.map(f)

  override def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S] = original.flatMap(f)

  override def flatten[S](implicit ev: T <:< Future[S]): Future[S] = original.flatten

  override def filter(p: T => Boolean)(implicit executor: ExecutionContext): Future[T] = original.filter(p)

  override def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext): Future[S] = original.collect(pf)

  override def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): Future[U] = original.recover(pf)

  override def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): Future[U] = original.recoverWith(pf)

  override def zip[U](that: Future[U]): Future[(T, U)] = original.zip(that)

  override def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext): Future[R] = original.zipWith(that)(f)

  override def fallbackTo[U >: T](that: Future[U]): Future[U] = original.fallbackTo(that)

  override def mapTo[S](implicit tag: ClassTag[S]): Future[S] = original.mapTo

  override def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): Future[T] = original.andThen(pf)

}
