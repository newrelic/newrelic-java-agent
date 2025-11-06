/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.org.apache.pekko.http

import org.apache.pekko.actor.{Actor, ActorRef, Props, Timers}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object StatusCheckActor {
  def props: Props = Props(new StatusCheckActor)

  case class Ping(id: Int)

  case object Pong

}

class StatusCheckActor extends Actor with Timers {

  import StatusCheckActor._

  val random: Random = Random
  var lastSender: Option[ActorRef] = None

  timers.startPeriodicTimer("my-timer", Pong, FiniteDuration(1, TimeUnit.SECONDS))

  override def receive: Receive = {
    case Ping(id) if id < 100 =>
      sender ! "Ping_OK"
    case Ping(id) =>
      lastSender = Some(sender)
    case Pong =>
      lastSender.foreach(_ ! "Pong_OK")
      lastSender = None
  }
}
