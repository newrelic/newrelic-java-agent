#Akka HTTP core instrumentation

##HttpExt Instrumentation
Instrumentation for Akka HTTP Core is carried out in the `akka.http.scaladsl.HttpExt` class that serves as the 
main entry point for a server. 2 convenience methods from `HttpExt` that can be used to start an HTTP server have 
been instrumented, they are :

- ` bindAndHandleAsync`: Convenience method which starts a new HTTP server at the given endpoint and uses handler that is a function recieving an `HttpRequest` and returning a `Future[HttpResponse]`
- ` bindAndHandleSync`: Convenience method which starts a new HTTP server at the given endpoint and uses handler that is a function recieving an `HttpRequest` and returning a `HttpResponse`


It has been decide that intrumentation is not extended for `bindAndHandle` which starts a new HTTP server using a 
`akka.stream.scaladsl.Flow` instance. This is due to a clash in the Akka Http Routing DSL instrumentation. 


Users wishing to start an HTTP Server from an `akka.stream.scaladsl.Flow` can use the following workaround

```scala
    val flow: Flow[HttpRequest, HttpResponse, NotUsed] = ???
    val asyncHandler: HttpRequest => Future[HttpResponse] = request => Source.single(request).via(flow).runWith(Sink.head)
    Http().bindAndHandleAsync(asyncHandler, host, port)
```

This workaround is not needed for users using  calling `bindAndHandle` using `akka.http.scaladsl.Route` from the 
Akka Http Routing DSL. Instrumentation should work in the same way being called from the other conveniencs methods 
to start an HTTP Server


