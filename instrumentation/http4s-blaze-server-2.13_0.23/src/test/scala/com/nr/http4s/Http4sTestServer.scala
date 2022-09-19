package com.nr.http4s

import cats.effect.IO
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import cats.effect.unsafe.implicits.global

class Http4sTestServer(val httpApp: HttpApp[IO]) {

  var server: Server = _
  var finalizer: IO[Unit] = _

  val serverResource = BlazeServerBuilder[IO]
    .withHttpApp(httpApp)
    .bindAny()
    .resource


  def start(): Unit = {
    val materializedServer = serverResource.allocated.unsafeRunSync()
    server = materializedServer._1
    finalizer = materializedServer._2
  }

  def stop(): Unit =
    finalizer.unsafeRunSync()

  def port: Int = server.address.getPort

  def hostname = server.address.getHostName
}
