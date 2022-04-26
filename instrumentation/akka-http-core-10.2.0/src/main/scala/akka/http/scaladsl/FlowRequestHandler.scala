/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package akka.http.scaladsl

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{Attributes, BidiShape, Inlet, Materializer, Outlet}
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.newrelic.agent.bridge.{AgentBridge, Token}
import com.newrelic.api.agent.{NewRelic, Trace, TransactionNamePriority}
import com.nr.instrumentation.akkahttpcore.{RequestWrapper, ResponseWrapper}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable


class FlowRequestHandler {

  // Since we reuse the same instrumentation to track Play, we are allowing for the component tag to be changed
  // temporarily.
  //  private val _serverInstrumentations = TrieMap.empty[Int, HttpServerInstrumentation]
  private val _defaultOperationNames = TrieMap.empty[Int, String]
  //  private val _defaultSettings = Settings("akka.http.server", "kamon.instrumentation.akka.http.server")
  //  @volatile private var _wrapperSettings = _defaultSettings

  def instrumentFlow(flow: Flow[HttpRequest, HttpResponse, Any]): Flow[HttpRequest, HttpResponse, Any] =
    BidiFlow.fromGraph(wrapStage).join(flow)

  def wrapStage =
    new GraphStage[BidiShape[HttpRequest, HttpRequest, HttpResponse, HttpResponse]] {
      val requestIn: Inlet[HttpRequest] = Inlet.create[HttpRequest]("request.in")
      val requestOut: Outlet[HttpRequest] = Outlet.create[HttpRequest]("request.out")
      val responseIn: Inlet[HttpResponse] = Inlet.create[HttpResponse]("response.in")
      val responseOut: Outlet[HttpResponse] = Outlet.create[HttpResponse]("response.out")
      val transactionCategory: String = "AkkaHttpCore"

      override val shape = BidiShape(requestIn, requestOut, responseIn, responseOut)

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
        private val tokensInFlight = mutable.Queue.empty[Token]
        //        private val _createdAt = Kamon.clock().instant()
        private var _completedRequests = 0

        setHandler(requestIn, new InHandler {
          override def onPush(): Unit = {
            val request = grab(requestIn)
            val token = fetchToken
            token.getTransaction.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, transactionCategory, "akkaHandler")
            NewRelic.getAgent.getTracedMethod.setMetricName("Akka", "RequestHandler")
            val wrappedRequest: RequestWrapper = new RequestWrapper(request)
            NewRelic.getAgent().getTransaction().setWebRequest(wrappedRequest)
            tokensInFlight.enqueue(token)

            push(requestOut, request)
          }


          override def onUpstreamFinish(): Unit =
            complete(requestOut)
        })

        setHandler(requestOut, new OutHandler {
          override def onPull(): Unit =
            pull(requestIn)

          override def onDownstreamFinish(): Unit =
            cancel(requestIn)
        })

        setHandler(responseIn, new InHandler {
          override def onPush(): Unit = {
            val response:HttpResponse = grab(responseIn)
            val token = tokensInFlight.dequeue()
            token.link()

            val txn = token.getTransaction
            if (txn != null) {
              val wrappedResponse = new ResponseWrapper(response)
              txn.setWebResponse(wrappedResponse)
              txn.addOutboundResponseHeaders()
              txn.markResponseSent()
              token.expire()

              val updatedResponse = wrappedResponse.response
              push(responseOut, updatedResponse)
            } else {
              push(responseOut, response)
            }
          }

          override def onUpstreamFinish(): Unit =
            completeStage()

        })

        setHandler(responseOut, new OutHandler {
          override def onPull(): Unit =
            pull(responseIn)

          override def onDownstreamFinish(): Unit =
            cancel(responseIn)
        })

        override def preStart(): Unit = {}

        override def postStop(): Unit = {
          //          val connectionLifetime = Duration.between(_createdAt, Kamon.clock().instant())
          //          httpServerInstrumentation.connectionClosed(connectionLifetime, _completedRequests)
        }
      }
    }

  @Trace(dispatcher = true)
  def fetchToken: Token = {
    AgentBridge.getAgent.getTransaction.getToken
  }
}