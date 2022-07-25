package com.nr.http4s

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import org.http4s.HttpApp
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

class Http4sTestServer(val httpApp: HttpApp[IO]) {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val concurrentEffect: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  var server: Server[IO] = _
  var finalizer: IO[Unit] = _

  val serverResource = BlazeServerBuilder[IO](global)
    .withHttpApp(httpApp)
    .bindAny()
    .resource


  def start(): Unit = {
    val materializedServer =  serverResource.allocated.unsafeRunSync()
    server = materializedServer._1
    finalizer = materializedServer._2
  }

  def stop(): Unit =
    finalizer.unsafeRunSync()

  def port: Int = server.address.getPort

  def hostname = server.address.getHostName
}
