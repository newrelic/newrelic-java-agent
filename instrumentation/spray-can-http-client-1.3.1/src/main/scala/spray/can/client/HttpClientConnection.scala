/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.can.client

import java.util.logging.Level

import akka.actor.Actor.Receive
import akka.actor._
import akka.io.Tcp
import com.newrelic.agent.bridge.AgentBridge
import com.newrelic.api.agent._
import com.newrelic.api.agent.weaver.{NewField, Weave, Weaver}
import com.nr.agent.instrumentation.spray.can.client.OutboundHttpHeaders
import spray.can.{Http, Http_Instrumentation}
import spray.http._
import spray.io._

@Weave
abstract class HttpClientSettingsGroup {
  def sender(): ActorRef

  def aroundReceive(receive :Receive, msg :Any) :Unit = {
    msg match {
      case connect: Http_Instrumentation.Connect if AgentBridge.getAgent.getTransaction(false) != null =>
        connect.token = NewRelic.getAgent.getTransaction.getToken
      case _ =>
    }
    Weaver.callOriginal()
  }
}

@Weave
abstract class HttpClientConnection (connectCommander: ActorRef,
                                   connect: Http.Connect,
                                   pipelineStage: RawPipelineStage[SslTlsContext],
                                   settings: ClientConnectionSettings) {
  // tcp connection is started in this constructor

  @NewField
  private val connectVal = connect
  @NewField
  private val isSSL = connect.sslEncryption
  @NewField
  private val remoteAddress = connect.remoteAddress

  // Internal akka method that we're shamelessly using.
  @Trace(async = true)
  def aroundReceive(receive :Receive, msg :Any) :Unit = {
    var connectInfo: Http_Instrumentation.Connect = connectVal.asInstanceOf[Http_Instrumentation.Connect]
    if (connectInfo.token != null) {
      connectInfo.token.linkAndExpire()
    }

    msg match {
      case request: HttpRequest_Instrumentation => {
        // The message, request, is immutable so we have to copy it then call back into this method with the new request
        // the outboundDone flag makes sure we only do this once
        if (!request.headersAdded) {
          val outbound: OutboundHttpHeaders = new OutboundHttpHeaders(request)
          val segment = NewRelic.getAgent.getTransaction.startSegment("SprayCanClient")
          segment.addOutboundRequestHeaders(outbound)
          aroundReceive(receive, outbound.getRequest(segment, remoteAddress, isSSL))
          return
        }
      }
      case _ =>
    }
    Weaver.callOriginal()
  }

}

