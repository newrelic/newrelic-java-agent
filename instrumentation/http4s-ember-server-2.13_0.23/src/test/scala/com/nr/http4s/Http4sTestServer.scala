package com.nr.http4s

import cats.effect.{IO}
import com.comcast.ip4s._
import org.http4s.HttpApp
import org.http4s.server.Server
import org.http4s.ember.server.EmberServerBuilder
import cats.effect.unsafe.implicits.global

class Http4sTestServer(val testServerHost: String,
                        val httpApp: HttpApp[IO]) {

  var server: Server = _
  var finalizer: IO[Unit] = _

  val serverResource =
    EmberServerBuilder.default[IO]
                      .withHttpApp(httpApp)
                      .withHost(Host.fromString(testServerHost).orNull)
                      .withPort(port"0")
                      .build

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
