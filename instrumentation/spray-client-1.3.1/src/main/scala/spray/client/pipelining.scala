/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package spray.client

import java.lang.Long
import java.net.URI
import java.util.{ArrayList, List}

import akka.actor.ActorRef
import akka.util.Timeout
import com.newrelic.agent.bridge.{AgentBridge, TracedActivity}
import com.newrelic.api.agent.{HttpParameters, Segment}
import com.newrelic.api.agent.weaver.{MatchType, Weave, Weaver}
import com.nr.agent.instrumentation.sprayclient._
import spray.http.{HttpHeader, HttpRequest, HttpResponse}

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success};

@Weave(`type` = MatchType.BaseClass, originalName = "spray.client.pipelining$")
class PipeliningInstrumentation {
  type SendReceive = HttpRequest ⇒ Future[HttpResponse]
  
  def sendReceive(transport: ActorRef)(implicit ec: ExecutionContext, futureTimeout: Timeout): SendReceive = {
    val orig :SendReceive = Weaver.callOriginal();
    util.makeWrapper(orig)
  }
}

object util {
  
   def getContentLength(outboundWrapper :OutboundWrapper ) :  Long = {
        val contentLength :String = outboundWrapper.getHeader("Content-Length");
        if (contentLength == null) {
           new Long(-1L);
        } else {
            Long.valueOf(contentLength);
        }
   }
   
  def makeWrapper(orig :HttpRequest ⇒ Future[HttpResponse])(implicit ec: ExecutionContext) :HttpRequest => Future[HttpResponse] = {
    val wrapped :HttpRequest => Future[HttpResponse] = (request :HttpRequest) => {
      if (null != AgentBridge.getAgent().getTransaction(false)) {
        val seg : Segment = AgentBridge.getAgent().getTransaction().createAndStartTracedActivity();
        if (seg != null) {
          var requestWithHeaders :HttpRequest = request
          var uri : String = null
          try {
            uri = request.uri.toString();
            val modifiableRequestHeaders :List[HttpHeader] = new ArrayList[HttpHeader](JavaConversions.asJavaCollection(request.headers));
            seg.addOutboundRequestHeaders(new OutboundWrapper(modifiableRequestHeaders));
            requestWithHeaders = request.withHeaders(JavaConversions.asScalaBuffer(modifiableRequestHeaders).toList);
          } catch {
            case t : Throwable => {
              AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle())
            }
          }
          // call the original sendReceive implementation
          val responseFuture :Future[HttpResponse] = orig(requestWithHeaders)

          responseFuture onComplete {
            case Success(response) => {
              try {
                seg.reportAsExternal(HttpParameters.library("SprayClient").uri(new URI(uri))
                    .procedure("sendReceive").inboundHeaders(new InboundWrapper(response.headers)).build());
                seg.end()
              } catch {
                case t :Throwable => {
                  AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle())
                }
              }
            }
            case Failure(t) => {
              try {
                seg.reportAsExternal(HttpParameters.library("SprayClient").uri(new URI(uri))
                  .procedure("sendReceiveOnFailure").noInboundHeaders().build());
                seg.end()
              } catch {
                case t :Throwable => {
                  AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle())
                }
              }
            }
          }
          responseFuture
        } else {
          orig(request)
        }
      } else {
        orig(request)
      }
    }
    return wrapped
  }
}
