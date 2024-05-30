#Pekko HTTP core instrumentation

This instrumentation is a lift of `akka-http-core-2.13_10.2.0`. 

As of `Pekko Http Core 1.0.0`, `bindAndHandleAsync` and `bindAndHandleSync` have both been deprecated and replaced by
`Http().newServerAt().bind()`. However, these methods still exist and are instrumented where used,
so the documentation below (also taken from Akka) is maintained for historical purposes. 


##HttpExt Instrumentation
Instrumentation for Pekko HTTP Core is carried out in the `pekko.http.scaladsl.HttpExt` class that serves as the 
main entry point for a server. 2 convenience methods from `HttpExt` that can be used to start an HTTP server have 
been instrumented, they are :

- ` bindAndHandleAsync`: Convenience method which starts a new HTTP server at the given endpoint and uses handler that is a function recieving an `HttpRequest` and returning a `Future[HttpResponse]`
- ` bindAndHandleSync`: Convenience method which starts a new HTTP server at the given endpoint and uses handler that is a function recieving an `HttpRequest` and returning a `HttpResponse`


It has been decided that intrumentation is not extended for `bindAndHandle` which starts a new HTTP server using a 
`pekko.stream.scaladsl.Flow` instance. This is due to a clash in the Akka Http Routing DSL instrumentation. 


Users wishing to start an HTTP Server from an `pekko.stream.scaladsl.Flow` can use the following workaround

```scala
    val flow: Flow[HttpRequest, HttpResponse, NotUsed] = ???
    val asyncHandler: HttpRequest => Future[HttpResponse] = request => Source.single(request).via(flow).runWith(Sink.head)
    Http().bindAndHandleAsync(asyncHandler, host, port)
```

This workaround is not needed for users using  calling `bindAndHandle` using `akka.http.scaladsl.Route` from the 
Pekko Http Routing DSL. Instrumentation should work in the same way being called from the other conveniencs methods 
to start an HTTP Server


