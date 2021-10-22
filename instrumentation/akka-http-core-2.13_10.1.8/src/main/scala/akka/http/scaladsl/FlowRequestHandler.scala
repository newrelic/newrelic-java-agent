/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Unzip, Zip}
import com.newrelic.agent.bridge.{AgentBridge, Token, TransactionNamePriority}
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.api.agent.{NewRelic, Trace}
import com.nr.instrumentation.akkahttpcore.{RequestWrapper, ResponseWrapper}


object FlowRequestHandler {

  val transactionCategory: String = "AkkaHttpCore"

  def instrumentFlow(handlerFlow: Flow[HttpRequest, HttpResponse, _]): Flow[HttpRequest, HttpResponse,
    NotUsed] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val enrichedInFlowShape = builder.add(Flow[HttpRequest].map(req => (createToken(req), req)))
        val removedOutFlowShape = builder.add(Flow[(Option[Token], HttpResponse)].map { case (optToken, resp) =>
          optToken.map(tkn => ResponseWrapper.wrapResponse(tkn, resp))
                  .getOrElse(resp)
        })

        val unzip = builder.add(Unzip[Option[Token], HttpRequest]())
        val zip = builder.add(Zip[Option[Token], HttpResponse]())
        val instrumentedFlow = builder.add(handlerFlow)

        enrichedInFlowShape ~> unzip.in
        unzip.out0 ~> zip.in0
        unzip.out1 ~> instrumentedFlow ~> zip.in1
        zip.out ~> removedOutFlowShape

        FlowShape(enrichedInFlowShape.in, removedOutFlowShape.out)
      }
    )

  @Trace(dispatcher = true)
  private def createToken(param: HttpRequest): Option[Token] = {
    try {
      val token = AgentBridge.getAgent.getTransaction.getToken
      AgentBridge.getAgent.getTransaction.setTransactionName(TransactionNamePriority.SERVLET_NAME, true, transactionCategory, "akkaHandler")
      NewRelic.getAgent.getTracedMethod.setMetricName("Akka", "RequestHandler")

      val wrappedRequest: RequestWrapper = new RequestWrapper(param)
      NewRelic.getAgent.getTransaction.setWebRequest(wrappedRequest)
      Some(token)
    } catch {
      case t: Throwable =>
        AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle)
        None
    }
  }
}
