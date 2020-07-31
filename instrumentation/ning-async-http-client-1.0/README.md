Ning Asynchronous HTTP Client Instrumentation
=============================================

## About Ning Asynchronous HTTP Client (AHC)

The Ning AHC is a popular API for asynchronously accessing remote resources via HTTP. The project is
hosted on Github: `https://github.com/AsyncHttpClient/async-http-client`. The versions have been consistently
tagged since version 1.0: `https://github.com/AsyncHttpClient/async-http-client/releases/tag/async-http-client-1.0.0`.
As of the writing of this document, there are 365 libraries which rely upon Ning 1.x, according to:
`http://mvnrepository.com/artifact/com.ning/async-http-client/usages`.

Ning is approximately 3.5x as popular as the next two most popular AHC libraries, Jetty's jetty-client and
Apache's httpasyncclient.

Ning 2.0 is currently under development...

This instrumentation provides support for Ning versions 1.0.0 through [1.x.x]...

## Using Ning in Client Application

Ning provides many examples in their javadocs for patterns on how Ning AHC may be both synchronously and
asychronously.

To use Ning AHC synchronously, execute the following:

    AsyncHttpClient c = new AsyncHttpClient();
    Future<Response> f = c.prepareGet("http://www.ning.com/").execute();
    Response response = f.get();

Which will block on the call to `f.get()` until the response is fully received.

To use Ning AHC asynchronously, implement an `AsyncCompletionHandler<Integer>`, providing an instance of one
to the `execute()` method, as so:

    AsyncHttpClient c = new AsyncHttpClient();
    Future<Response> f = c.prepareGet("http://www.ning.com/").execute(new AsyncCompletionHandler<Response>() {
        @Override
        public Response onCompleted(Response response) throws IOException {
            // Do something
            return response;
        }

        @Override
        public void onThrowable(Throwable t) {
            // Handle the exception
        }
    });
    Response response = f.get();

Ning AHC also allows the programmer to control how the response is parsed. Instead of simply returning a bare
`Response` object, the `AsyncCompletionHandler` may return a different type:

     // We are just interested to retrieve the status code.
    Future<Integer> f = c.prepareGet("http://www.ning.com/").execute(new AsyncCompletionHandler<Integer>() {
        @Override
        public Integer onCompleted(Response response) throws IOException {
            // Do something
            return response.getStatusCode();
        }

        @Override
        public void onThrowable(Throwable t) {
            // Handle the exception
        }
    });
    Integer statusCode = f.get();

Ning AHC also provides a more advanced means to control how the response is asynchronously processed, through
the callbacks defined in `AsyncHandler`.

    AsyncHttpClient c = new AsyncHttpClient();
    Future<String> f = c.prepareGet("http://www.ning.com/").execute(new AsyncHandler<String>() {
        private StringBuilder builder = new StringBuilder();

        @Override
        public STATE onStatusReceived(HttpResponseStatus s) throws Exception {
            // return STATE.CONTINUE or STATE.ABORT
            return STATE.CONTINUE
        }

        @Override
        public STATE onHeadersReceived(HttpResponseHeaders bodyPart) throws Exception {
            // return STATE.CONTINUE or STATE.ABORT
            return STATE.CONTINUE
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            builder.append(new String(bodyPart));
            // return STATE.CONTINUE or STATE.ABORT
            return STATE.CONTINUE
        }

        @Override
        public String onCompleted() throws Exception {
            // Will be invoked once the response has been fully read or a ResponseComplete exception
            // has been thrown.
            return builder.toString();
        }

        @Override
        public void onThrowable(Throwable t) {
            // Handle the exception
        }
    });
    String bodyResponse = f.get();

## Ning Implementation

Ning delegates the low-level NIO work to another NIO library, and allows the programmer to configure which is used.
In version 1.0.0, the only implementation provided is Netty. Later versions provide Grizzly, and a "plain" JDK
implementation.

*TODO:* Answer, "Do we need to capture & highlight which NIO provider is being used for our clients?"

We _likely_ use instances of Request as a key in the map to track the start of the external call. There is a minor
risk that a well-meaning, but devious, programmer may wish to create a requests for themselves. Should they choose
to not create a new `Request` instance on each call to `public Request build()`, such as to cache and reuse the
request, our instrumentation _may_ have problems, unless we choose to

It is completely permissable to have multiple instances of a `AsyncHttpClient`, *DO NOT* assume the one you're
instrumenting is the only one. Additionally, they may each be uniquely configured.

## Ning Instrumentation

### Sending a Request

Ning 1.0.0 provides a simplistic request-based all-at-once means to send a request. In this version, Ning requires
that the entire request be loaded into memory at once. This is obviously a pain point in cases where the request being
submitted such as during the upload of a large file. Later versions support more complex workflows which allow the
client to only load a small part of the request into memory at a time.

It is believed that HTTP1.1 (not WebSockets) specifies the server should not send a response until the entire request
body is received. For this (and many other) reason, external activity metric measures the time from first byte sent to
first byte received.

### Processing a Response

Ning provides several callback methods which let the programmer control how a response is processed. The key callback
methods are:

    public interface AsyncHandler<T> {
        public static enum STATE { ABORT, CONTINUE }

        // Called first, allows early termination based on HTTP response code, e.g. on 400, 404, 500, 501, etc.
        STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception;

        // Called second, allows early termination based on HTTP headers.
        STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception;

        // Called third, when the body contents of the HTTP response are received.
        STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception;

        // Called fourth, (only once?) when the HTTP response has been completely received.
        T     onCompleted() throws Exception;

        // Called at any point in the processing, possibly before any of the others are called.
        void  onThrowable(Throwable t);
    }

    public abstract class AsyncCompletionHandler<T> implements AsyncHandler<T> {

        // Called after onBodyPartReceived() and before T onCompleted(), allowing the user code to define custom
        // processing of the Response rather than simply returning a plain HTTP Response. One common use case is
        // to parse the Response text as JSON, creating a domain object from the JSON.
        abstract public T onCompleted(Response response) throws Exception;
    }

Our asynchronous instrumentation needs to be respectful of the user's code in this workflow, so that if the user's code
decides to terminate early, we must handle that gracefully. Our instrumentation should intercept and do its work before
the user's callback is invoked. That way, we can avoid any user-code induced locking or other weirdness.

The basic call-flow is (time along the x axis):

     http/200
     response                                 ContentLength: 1234
      code                                    MyHeader: {...}
       \ /                                          \ /
        V                                     ^ .... V                                      ^ ....
        | ning.handleUpstream()               |      | ning.handleUpstream()                |
        +----+                              +-+      +----+                              +-+
             | handler.onStatusReceived()   |             | handler.onHeadersReceived()  |
             +----+                       +-+             +----+                       +-+
                  | user.code()           |                    | user.code()           |
                  +--* return CONTINUE;---+                    +--* return CONTINUE;---+
                     * return ABORT;----+                         * return ABORT;----+
                                        |                                            |
                                        V                                            V
                                     no more                                      no more
                                    processing                                   processing

### Handling Persistent HTTP connections

Ning supports HTTP chunked messages, aka HTTP/KeepAlive connections, which is why the method is called
`onBodyPartReceived(...)` not `onBodyReceived(...)`. This is not quite the same as a WebSocket. Tracking Websockets is
not supported by NewRelic currently, so we don't have to worry about it yet. Support for WebSockets was not added to
netty until 1.7.??. If/When we do need to support WebSockets, the poor, downtrodden instrumentation developer will have
to add instrumentation to WebSocketUpgradeHandler. Good luck with that!

According to Netty's documentation, chunks are received and passed along for processing in the order they are received,
so there needn't be excessive handwringing about the ordering. One curiosity is that each `HttpChunk` can be appended
with a `HttpChunkTrailer` containing updates to the header. Thus, it is possible that in a KeepAlive HTTP connection,
the call sequence will start the same, but contain multiple calls to the `handler.onBodyPartReceived(...)`, each of
which may (or may not) be followed by a call to `handler.onHeadersReceived(...)`.

    // TODO Create a code-flow graph illustrating the flow of a HTTP/KeepAlive connection.

SPECULATIVE: To handle this, we're going to need additional logic to be sure we don't reply with the external metric
timing on receipt of each HttpChunkTrailer ... which is what will induce additional calls to `onHeadersReceived(...)`.

### Thread Safety

Ning doesn't make particularly detailed claims on the timing and ordering of methods. This may change in later versions,
but as of 1.0.0, Ning simply passes along the thread safety of the provider. In 1.0.0, Netty is the default (and only)
provider, so the behavior will be consistent, regardless of configuration. *Caution*: Later versions, which may be
configured to use several different providers, _may not_ exhibit consistent thread-safety behavior across all
configurations.

Ning's Netty-based Provider implements Netty's ChannelUpstreamHandler which provides callbacks for Netty-driven
upstream (incoming) channel events. According to Netty's documentation
(see `https://docs.jboss.org/netty/3.2/api/org/jboss/netty/channel/ChannelUpstreamHandler.html`) events will be invoked
sequentially by the same thread, and thus Ning's each `AsyncHandler` callback method will always be invoked _after_ the
previous callback method has completed.

As of 1.0.0, Ning does not use an `ExecutionHandler` on a `ChannelPipeline`. It simply uses Netty's default, which is
a single-thread of execution for the upstream (inbound) event notification to the `ChannelUpstreamHandler`. If Ning
changes to use a multithreaded or pooling `Executor` in later versions, or even allows the user to configure how many
threads are used, we may need to rework the instrumentation to handle the same `AsyncHandler` being called from
multiple threads. Keep an eye out for this in later versions, as this may change the behavior depending on which
`Executor` is used to dispatch the events. See
`https://docs.jboss.org/netty/3.2/api/org/jboss/netty/handler/execution/ExecutionHandler.html` for more details.

## Summary of Version-over-Version Changes

### From—To, Type {+, -, ~}, Class or Method<br/>

#### 1.0.0—1.1.0<tab/><tab/><br/>
<tab/><tab/>~ com.ning.http.client.AsyncHttpClient$BoundRequestBuilder<br/>
<tab/><tab/><tab/><tab/>~ setHeaders(Headers) -> setHeaders(FluentCaseInsensitiveStringsMap)<br/>
<tab/><tab/><tab/><tab/>~ setParameters(Multimap<String, String>) -> setParameters(FluentStringsMap)<br/>
<tab/><tab/><tab/><tab/>+ setSignatureCalculator(SignatureCalculator)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>+ setSignatureCalculator(SignatureCalculator)<br/>
<tab/><tab/><tab/><tab/>+ requestBuilder(Request)<br/>
<tab/><tab/><tab/><tab/>+ requestBuilder(RequestType, String)<br/>

#### 1.1.0—1.2.0<br/>

<tab/><tab/>~ com.ning.http.client.AsyncCompletionHandler<br/>
<tab/><tab/><tab/><tab/>+ onHeaderWriteCompleted()<br/>
<tab/><tab/><tab/><tab/>+ onContentWriteCompleted()<br/>
<tab/><tab/><tab/><tab/>+ onContentWriteProgress(long, long, long)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>+ prepareConnect(String)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>+ getSSLContext()<br/>
<tab/><tab/><tab/><tab/>+ getConnectionsPool()<br/>
<tab/><tab/><tab/><tab/>+ getSSLEngineFactory()<br/>
<tab/><tab/><tab/><tab/>+ getAsyncHttpProviderConfig()<br/>
<tab/><tab/><tab/><tab/>+ setSSLContext(SSLContext)<br/>
<tab/><tab/><tab/><tab/>+ setAsyncHttpProviderConfig(AsyncHttpProviderConfig&lt;?,?&gt;)<br/>
<tab/><tab/><tab/><tab/>+ setConnectionsPool(ConnectionsPool&lt;?,?&gt;)<br/>

<tab/><tab/>+ com.ning.http.client.AsyncHttpProviderConfig&lt;U,V&gt;<br/>

<tab/><tab/>+ com.ning.http.client.ConnectionsPool&lt;U,V&gt;<br/>

<tab/><tab/>~ com.ning.http.client.Cookie&lt;U,V&gt;<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;(String, String, String, String, int, boolean, int)<br/>
<tab/><tab/><tab/><tab/>+ getVersion()<br/>
<tab/><tab/><tab/><tab/>+ getPorts()<br/>
<tab/><tab/><tab/><tab/>+ setPorts(int...)<br/>
<tab/><tab/><tab/><tab/>+ setPorts(Iterable&lt;Integer&gt;)<br/>

<tab/><tab/>~ com.ning.http.client.HttpContent<br/>
<tab/><tab/><tab/><tab/>~ provider()<br/>

<tab/><tab/>~ com.ning.http.client.HttpResponseBodyPart<br/>
<tab/><tab/><tab/><tab/>+ writeTo(OutputStream)<br/>
<tab/><tab/><tab/><tab/>+ getBodyByteBuffer()<br/>

<tab/><tab/>+ com.ning.http.client.PerRequestConfig<br/>
<tab/><tab/>+ com.ning.http.client.ProgressAsyncHandler<br/>

<tab/><tab/>~ com.ning.http.client.ProxyServer<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;(Protocol, String, int, String, String)<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;(String, int, String, String)<br/>
<tab/><tab/><tab/><tab/>+ getPrincipal()<br/>
<tab/><tab/><tab/><tab/>+ getPassword()<br/>


<tab/><tab/>~ com.ning.http.client.Realm<br/>
<tab/><tab/><tab/><tab/>+ getUsePreemptiveAuth()<br/>
<tab/><tab/><tab/><tab/>+ getDomain()<br/>

<tab/><tab/>~ com.ning.http.client.Realm$AuthScheme<br/>
<tab/><tab/><tab/><tab/>+ NTLM<br/>

<tab/><tab/>~ com.ning.http.client.Realm$RealmBuilder<br/>
<tab/><tab/><tab/><tab/>+ getDomain()<br/>
<tab/><tab/><tab/><tab/>+ setDomain(String)<br/>
<tab/><tab/><tab/><tab/>+ getPreemptiveAuth()<br/>
<tab/><tab/><tab/><tab/>+ setUsePreemptiveAuth(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.Request<br/>
<tab/><tab/><tab/><tab/>+ getFile()<br/>
<tab/><tab/><tab/><tab/>+ isRedirectEnabled()<br/>
<tab/><tab/><tab/><tab/>+ getPerRequestConfig()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilder<br/>
<tab/><tab/><tab/><tab/>+ setFollowRedirects(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase$RequestImpl<br/>
<tab/><tab/><tab/><tab/>~ getType() -> getReqType()<br/>
<tab/><tab/><tab/><tab/>+ getFile()<br/>
<tab/><tab/><tab/><tab/>+ isRedirectEnabled()<br/>
<tab/><tab/><tab/><tab/>+ getPerRequestConfig()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase<br/>
<tab/><tab/><tab/><tab/>~ &lt;init&gt;(Class<T>, RequestType) -> &lt;init&gt;(Class&lt;T&gt;, String)<br/>
<tab/><tab/><tab/><tab/>+ setBody(File)<br/>
<tab/><tab/><tab/><tab/>+ setFollowRedirects(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setPerRequestConfig(PerRequestConfig)<br/>

<tab/><tab/>~ com.ning.http.client.Response<br/>
<tab/><tab/><tab/><tab/>+ hasResponseStatus()<br/>
<tab/><tab/><tab/><tab/>+ hasResponseHeaders()<br/>
<tab/><tab/><tab/><tab/>+ hasResponseBody()<br/>

<tab/><tab/>+ com.ning.http.client.SSLEngineFactory<br/>

<tab/><tab/>+ com.ning.http.client.providers.jdk.JDKAsyncHttpProvider<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.JDKAsyncHttpProviderConfig<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.JDKConnectionsPool<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.JDKFuture<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.JDKResponse<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.ResponseBodyPart<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.ResponseHeaders<br/>
<tab/><tab/>+ com.ning.http.client.providers.jdk.ResponseStatus<br/>

<tab/><tab/>+ com.ning.http.client.providers.netty.ConnectionListener<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.NettyAsyncHttpProvider<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.NettyConnectionsPool<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.NettyResponseFuture<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.ResponseBodyPart<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.ResponseHeaders<br/>
<tab/><tab/>+ com.ning.http.client.providers.netty.ResponseStatus<br/>

<tab/><tab/>+ com.ning.http.client.webdav.WebDavCompletionHandlerBase<br/>
<tab/><tab/>+ com.ning.http.client.webdav.WebDavResponse<br/>

<tab/><tab/>+ com.ning.http.util.AsyncHttpProviderUtils<br/>

<tab/><tab/>~ com.ning.http.util.AuthenticatorUtils<br/>
<tab/><tab/><tab/><tab/>+ computeBasicAuthentication(ProxyServer)<br/>

<tab/><tab/>~ com.ning.http.util.SslUtils<br/>
<tab/><tab/><tab/><tab/>+ getSSLContext()<br/>

#### 1.2.0 — 1.3.4

<tab/><tab/>~ com.ning.http.client.AsyncContentHandler<br/>
<tab/><tab/><tab/><tab/>- onContentWriteProgess(long, long, long)<br/>
<tab/><tab/><tab/><tab/>+ onContentWriteProgress(long, long, long)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>~ setSignatureCalculator(SignatureCalculator)<br/>
<tab/><tab/><tab/><tab/>+ addAsyncFilter(AsyncFilter)<br/>
<tab/><tab/><tab/><tab/>+ removeAsyncFilter(AsyncFilter)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>~ getRealm()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>~ setRealm(Realm)<br/>

<tab/><tab/>~ com.ning.http.client.FutureImpl<br/>
<tab/><tab/><tab/><tab/>~ done(Callable)<br/>

<tab/><tab/>~ com.ning.http.client.ProgressAsyncHandler<br/>
<tab/><tab/><tab/><tab/>- onContentWriteProgess(long, long, long)<br/>
<tab/><tab/><tab/><tab/>+ onContentWriteProgress(long, long, long)<br/>

<tab/><tab/>~ com.ning.http.client.ProxyServer<br/>
<tab/><tab/><tab/><tab/>~ getEncoding()<br/>
<tab/><tab/><tab/><tab/>~ setEncoding(String)<br/>

<tab/><tab/>~ com.ning.http.client.Realm<br/>
<tab/><tab/><tab/><tab/>+ getEncoding()<br/>

<tab/><tab/>~ com.ning.http.client.Realm$RealmBuilder<br/>
<tab/><tab/><tab/><tab/>+ getEncoding()<br/>
<tab/><tab/><tab/><tab/>+ setEncoding(String)<br/>

<tab/><tab/>~ com.ning.http.client.Request<br/>
<tab/><tab/><tab/><tab/>+ getBodyGenerator()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase<br/>
<tab/><tab/><tab/><tab/>+ getBodyGenerator()<br/>
<tab/><tab/><tab/><tab/>+ setBodyGenerator()<br/>

<tab/><tab/>+ com.ning.http.client.Response$ResponseBuilder<br/>
<tab/><tab/>+ com.ning.http.client.Body<br/>
<tab/><tab/>+ com.ning.http.client.BodyGenerator<br/>
<tab/><tab/>+ com.ning.http.client.FileBodyGenerator<br/>
<tab/><tab/>+ com.ning.http.client.RandomAccessBody<br/>

<tab/><tab/>+ com.ning.http.client.filter.AsyncFilter<br/>
<tab/><tab/>+ com.ning.http.client.filter.AsyncFilterContext<br/>
<tab/><tab/>+ com.ning.http.client.filter.AsyncFilterException<br/>
<tab/><tab/>+ com.ning.http.client.filter.ThrottleRequestAsyncFilter<br/>

<tab/><tab/>+ com.ning.http.util.UTF8Codec<br/>

#### 1.3.4 — 1.4.1

<tab/><tab/>~ com.ning.http.client.AsyncCompletionHandler<br/>
<tab/><tab/><tab/><tab/>+ onStatusReceived(HttpResponseStatus)<br/>
<tab/><tab/><tab/><tab/>+ onHeadersReceived(HttpResponseHeaders)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>- addAsyncFilter(AsyncFilter)<br/>
<tab/><tab/><tab/><tab/>+ removeAsyncFilter(AsyncFilter)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>+ getAllowPoolingConnection()<br/>
<tab/><tab/><tab/><tab/>+ getRequestFilters()<br/>
<tab/><tab/><tab/><tab/>+ getResponseFilters()<br/>
<tab/><tab/><tab/><tab/>+ getIOExceptionFilters()<br/>
<tab/><tab/><tab/><tab/>+ getRequestCompressionLevel()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>+ setAllowPoolingConnection(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setKeepAlive(boolean)<br/>
<tab/><tab/><tab/><tab/>+ addRequestFilter(RequestFilter)<br/>
<tab/><tab/><tab/><tab/>+ removeRequestFilter(RequestFilter)<br/>
<tab/><tab/><tab/><tab/>+ addResponseFilter(ResponseFilter)<br/>
<tab/><tab/><tab/><tab/>+ removeResponseFilter(ResponseFilter)<br/>
<tab/><tab/><tab/><tab/>+ addIOExceptionFilter(IOExceptionFilter)<br/>
<tab/><tab/><tab/><tab/>+ removeIOExceptionFilter(IOExceptionFilter)<br/>
<tab/><tab/><tab/><tab/>+ getRequestCompressionLevel()<br/>
<tab/><tab/><tab/><tab/>+ setRequestCompressionLevel(int)<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;(AsyncHttpClientConfig)<br/>
<tab/><tab/><tab/><tab/>+ isResumableDownload()<br/>
<tab/><tab/><tab/><tab/>+ setResumableDownload(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.ConnectionsPool&lt;U, V&gt;<br/>
<tab/><tab/><tab/><tab/>- addConnection(U,V)<br/>
<tab/><tab/><tab/><tab/>- getConnection(U)<br/>
<tab/><tab/><tab/><tab/>- removeConnection(U)<br/>
<tab/><tab/><tab/><tab/>- removeAllConnection(V)<br/>
<tab/><tab/><tab/><tab/>+ offer(U,V)<br/>
<tab/><tab/><tab/><tab/>+ poll(U)<br/>
<tab/><tab/><tab/><tab/>+ removeAll(V)<br/>

<tab/><tab/>~ com.ning.http.client.FutureImpl&lt;V&gt;<br/>
<tab/><tab/><tab/><tab/>+ content(V)<br/>
<tab/><tab/><tab/><tab/>+ touch()<br/>

<tab/><tab/>~ com.ning.http.client.Request<br/>
<tab/><tab/><tab/><tab/>+ getRangeOffset()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase<br/>
<tab/><tab/><tab/><tab/>+ getRangeOffset()<br/>
<tab/><tab/><tab/><tab/>+ setRangeOffset(long)<br/>

<tab/><tab/>~ com.ning.http.client.Response<br/>
<tab/><tab/><tab/><tab/>+ reset()<br/>

<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheAsyncHttpProvider<br/>
<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheAsyncHttpProviderConfig<br/>
<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheResponse<br/>
<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheResponseBodyPart<br/>
<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheResponseFuture<br/>
<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheResponseHeaders<br/>
<tab/><tab/>+ com.ning.http.client.providers.apache.ApacheResponseStatus<br/>

#### 1.4.1 — 1.5.0

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>+ getIdleConnectionTimeInMs()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>+ setIdleConnectionTimeInMs(int)<br/>
<tab/><tab/><tab/><tab/>- isResumableDownload()<br/>
<tab/><tab/><tab/><tab/>- setResumableDownload(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.FutureImpl&lt;V&gt;<br/>
<tab/><tab/><tab/><tab/>+ getAndSetWriteHeaders(boolean)<br/>
<tab/><tab/><tab/><tab/>+ getAndSetWriteBody(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.Request<br/>
<tab/><tab/><tab/><tab/>+ getMethod()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilder<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;()<br/>
<tab/><tab/><tab/><tab/>+ setMethod(long)<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase<br/>
<tab/><tab/><tab/><tab/>+ getMethod()<br/>
<tab/><tab/><tab/><tab/>+ setMethod(long)<br/>

<tab/><tab/>+ com.ning.http.client.SimpleAsyncHttpClient<br/>
<tab/><tab/>+ com.ning.http.client.ThrowableHandler<br/>

#### 1.5.0 — 1.6.5

<tab/><tab/>~ com.ning.http.client.AsyncHandler$STATE<br/>
<tab/><tab/><tab/><tab/>+ PAUSE<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>~ execute(AsyncHandler&lt;T&gt;)<br/>
<tab/><tab/><tab/><tab/>~ execute()<br/>
<tab/><tab/><tab/><tab/>~ executeRequest(Request, AsyncHandler&lt;T&gt;)<br/>
<tab/><tab/><tab/><tab/>~ executeRequest(Request)<br/>
<tab/><tab/><tab/><tab/>+ getProvider()<br/>
<tab/><tab/><tab/><tab/>+ isClosed()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;()<br/>
<tab/><tab/><tab/><tab/>+ getMaxRequestRetry()<br/>
<tab/><tab/><tab/><tab/>+ isSslConnectionPoolEnabled()<br/>
<tab/><tab/><tab/><tab/>+ isUseRawUrl()<br/>
<tab/><tab/><tab/><tab/>+ isRemoveQueryParamOnRedirect()<br/>
<tab/><tab/><tab/><tab/>+ isClosed()<br/>
<tab/><tab/><tab/><tab/>+ getHostnameVerifier()<br/>
<tab/><tab/><tab/><tab/>+ getIoThreadMultiplier()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>+ setMaxRequestRetry(int)<br/>
<tab/><tab/><tab/><tab/>+ setAllowSslConnectionPool(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setUseRawUrl(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setRemoveQueryParamOnRedirect(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setUseProxyProperties(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setIOThreadMultiplier(int)<br/>
<tab/><tab/><tab/><tab/>+ setHostnameVerifier(HostnameVerifier)<br/>

<tab/><tab/>+ com.ning.http.client.AsyncHttpClientConfigBean<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpListener<br/>
<tab/><tab/><tab/><tab/>+ execute(Request, AsyncHandler&lt;T&gt;)<br/>

<tab/><tab/>+ com.ning.http.client.BodyDeferringAsyncHandler<br/>

#### 1.6.5 — 1.7.24

<tab/><tab/>~ com.ning.http.client.AsyncHandler$STATE<br/>
<tab/><tab/><tab/><tab/>- PAUSE<br/>
<tab/><tab/><tab/><tab/>+ UPGRADE<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>+ closeAsynchronously()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>+ getWebSocketIdleTimeoutInMs()<br/>
<tab/><tab/><tab/><tab/>+ getProxyServerSelector()<br/>
<tab/><tab/><tab/><tab/>+ isValid()<br/>
<tab/><tab/><tab/><tab/>+ isStrict302Handling()<br/>
<tab/><tab/><tab/><tab/>+ isUseRelativeURIsWithSSLProxies()<br/>
<tab/><tab/><tab/><tab/>+ getMaxConnectionLifeTimeInMs()<br/>
<tab/><tab/><tab/><tab/>+ isRfc6265CookieEncoding()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>+ setWebSocketIdleTimeoutInMs(int)<br/>
<tab/><tab/><tab/><tab/>+ setProxyServerSelector()<br/>
<tab/><tab/><tab/><tab/>+ setRemoveQueryParamsOnRedirect(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setUseProxySelector(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setStrict302Handling(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setUseRelativeURIsWithSSLProxies(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setMaxConnectionLifeTimeInMs(int)<br/>
<tab/><tab/><tab/><tab/>+ setRfc6265CookieEncoding(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfigBean<br/>
<tab/><tab/><tab/><tab/>+ setIdleConnectionTimeoutInMs(int)<br/>
<tab/><tab/><tab/><tab/>+ setProxyServerSelector(ProxyServerSelector)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpProvider<br/>
<tab/><tab/><tab/><tab/>~ prepareResponse(HttpResponseStatus, HttpResponseHeaders, List&lt;HttpResponseBodyPart&gt;)<br/>

<tab/><tab/>~ com.ning.http.client.Cookie<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;(String, String, String, String, String, int, boolean, int, boolean, boolean, String, String, Iterable&lt;Integer&gt;)<br/>
<tab/><tab/><tab/><tab/>+ getRawValue()<br/>
<tab/><tab/><tab/><tab/>+ getComment()<br/>
<tab/><tab/><tab/><tab/>+ getCommentUrl()<br/>
<tab/><tab/><tab/><tab/>+ isHttpOnly()<br/>
<tab/><tab/><tab/><tab/>+ isDiscard()<br/>
<tab/><tab/><tab/><tab/>+ compareTo(Cookie)<br/>

<tab/><tab/>~ com.ning.http.client.HttpResponseBodyPart<br/>
<tab/><tab/><tab/><tab/>+ isLast()<br/>
<tab/><tab/><tab/><tab/>+ markUnderlyingConnectionAsClosed()<br/>
<tab/><tab/><tab/><tab/>+ closeUnderlyingConnection()<br/>

<tab/><tab/>+ com.ning.http.client.HttpResponseBodyPartInputStream<br/>

<tab/><tab/>~ com.ning.http.client.ListenableFuture&lt;V&gt;<br/>
<tab/><tab/><tab/><tab/>- done(Callable)<br/>
<tab/><tab/><tab/><tab/>+ done()<br/>

<tab/><tab/>~ com.ning.http.client.ProxyServer<br/>
<tab/><tab/><tab/><tab/>+ getURI()<br/>

<tab/><tab/>+ com.ning.http.client.ProxyServerSelector<br/>

<tab/><tab/>~ com.ning.http.client.Realm<br/>
<tab/><tab/><tab/><tab/>+ getOpaque()<br/>

<tab/><tab/>~ com.ning.http.client.Realm$RealmBuilder<br/>
<tab/><tab/><tab/><tab/>+ getOpaque()<br/>
<tab/><tab/><tab/><tab/>+ setOpaque(String)<br/>

<tab/><tab/>~ com.ning.http.client.Request<br/>
<tab/><tab/><tab/><tab/>+ getOriginalURI()<br/>
<tab/><tab/><tab/><tab/>+ getURI()<br/>
<tab/><tab/><tab/><tab/>+ getRawURI()<br/>
<tab/><tab/><tab/><tab/>+ getInetAddress()<br/>
<tab/><tab/><tab/><tab/>+ getLocalAddress()<br/>
<tab/><tab/><tab/><tab/>+ isRedirectOverrideSet()<br/>
<tab/><tab/><tab/><tab/>+ isUseRawUrl()<br/>
<tab/><tab/><tab/><tab/>+ getConnectionPoolKeyStrategy()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilder<br/>
<tab/><tab/><tab/><tab/>+ &lt;init&gt;(String, boolean)<br/>
<tab/><tab/><tab/><tab/>+ getOriginalURI()<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase<br/>
<tab/><tab/><tab/><tab/>+ setURI(URI)<br/>
<tab/><tab/><tab/><tab/>+ setInetAddress(InetAddress)<br/>
<tab/><tab/><tab/><tab/>+ setLocalAddress(InetAddress)<br/>
<tab/><tab/><tab/><tab/>+ setConnectionPoolKeyStrategy(ConnectionPoolKeyStrategy)<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase$RequestImpl<br/>
<tab/><tab/><tab/><tab/>+ getOriginalURI()<br/>
<tab/><tab/><tab/><tab/>+ getURI()<br/>
<tab/><tab/><tab/><tab/>+ getRawURI()<br/>
<tab/><tab/><tab/><tab/>+ getInetAddress()<br/>
<tab/><tab/><tab/><tab/>+ getLocalAddress()<br/>
<tab/><tab/><tab/><tab/>+ isRedirectOverrideSet()<br/>
<tab/><tab/><tab/><tab/>+ isUseRawUrl()<br/>
<tab/><tab/><tab/><tab/>+ getConnectionPoolKeyStrategy()<br/>

<tab/><tab/>~ com.ning.http.client.Response<br/>
<tab/><tab/><tab/><tab/>+ getResponseBodyAsBytes()<br/>
<tab/><tab/><tab/><tab/>+ getResponseBodyAsByteBuffer()<br/>

<tab/><tab/>~ com.ning.http.client.SimpleAsyncHttpClient$Builder<br/>
<tab/><tab/><tab/><tab/>+ setProviderClass(String)<br/>
<tab/><tab/><tab/><tab/>+ getResponseBodyAsByteBuffer()<br/>

<tab/><tab/>+ com.ning.http.client.UpgradeHandler<br/>

<tab/><tab/>+ com.ning.http.client.providers.grizzly.*<br/>
<tab/><tab/>+ com.ning.http.client.websocket.*<br/>

#### 1.7.24 — 1.8.16

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>- reaper()<br/>
<tab/><tab/><tab/><tab/>+ isUseRelativeURIsWithConnectProxies()<br/>
<tab/><tab/><tab/><tab/>- isRfc6265CookieEncoding()<br/>
<tab/><tab/><tab/><tab/>+ getTimeConverter()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>- setScheduledExecutorService(ScheduledExecutorService)<br/>
<tab/><tab/><tab/><tab/>+ setUseRelativeURIsWithConnectProxies(boolean)<br/>
<tab/><tab/><tab/><tab/>- setRfc6265CookieEncoding(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setTimeConverter(TimeConverter)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfigBean<br/>
<tab/><tab/><tab/><tab/>+ setStrict302Handling(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setMaxConnectionLifeTimeInMs(int)<br/>
<tab/><tab/><tab/><tab/>- setReaper(ScheduledExecutorService)<br/>

<tab/><tab/>+ com.ning.http.client.AsyncHttpClientConfigDefaults<br/>
<tab/><tab/>- com.ning.http.client.Cookie<br/>
<tab/><tab/>+ com.ning.http.client.cookie.Cookie<br/>
<tab/><tab/>+ com.ning.http.client.cookie.CookieDecoder<br/>
<tab/><tab/>+ com.ning.http.client.cookie.CookieEncoder<br/>

<tab/><tab/>~ com.ning.http.client.HttpResponseBodyPart<br/>

<tab/><tab/>~ com.ning.http.client.ProxyServer<br/>
<tab/><tab/><tab/><tab/>+ getUrl()<br/>

<tab/><tab/>~ com.ning.http.client.Realm<br/>
<tab/><tab/><tab/><tab/>+ isUseAbsoluteURI()<br/>
<tab/><tab/><tab/><tab/>+ isOmitQuery()<br/>

<tab/><tab/>~ com.ning.http.client.Realm$RealmBuilder<br/>
<tab/><tab/><tab/><tab/>+ isUseAbsoluteURI()<br/>
<tab/><tab/><tab/><tab/>+ setUseAbsoluteURI(boolean)<br/>
<tab/><tab/><tab/><tab/>+ isOmitQuery()<br/>
<tab/><tab/><tab/><tab/>+ setOmitQuery(boolean)<br/>

<tab/><tab/>~ com.ning.http.client.RequestBuilderBase<br/>
<tab/><tab/><tab/><tab/>+ setCookies(Collection&lt;Cookie&gt;)()<br/>
<tab/><tab/><tab/><tab/>+ addOrReplaceCookie(Cookie)()<br/>
<tab/><tab/><tab/><tab/>+ resetCookies()<br/>
<tab/><tab/><tab/><tab/>+ resetQueryParameters()<br/>
<tab/><tab/><tab/><tab/>+ setSignatureCalculator(SignatureCalculator)<br/>

<tab/><tab/>~ com.ning.http.client.SimpleAsyncHttpClient<br/>
<tab/><tab/><tab/><tab/>- setScheduledExecutorService(ScheduledExcecutorService)<br/>

#### 1.8.16 — 1.9.31

<tab/><tab/>~ com.ning.http.client.AsyncHandlerExtensions<br/>
<tab/><tab/><tab/><tab/>+ onOpenConnection()<br/>
<tab/><tab/><tab/><tab/>+ onConnectionOpen()<br/>
<tab/><tab/><tab/><tab/>+ onPoolConnection()<br/>
<tab/><tab/><tab/><tab/>+ onConnectionPooled()<br/>
<tab/><tab/><tab/><tab/>~ onSendRequest(Object)<br/>
<tab/><tab/><tab/><tab/>+ onDnsResolved(InetAddress)<br/>
<tab/><tab/><tab/><tab/>+ onSslHandshakeCompleted(InetAddress)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClient$BoundRequestBuilder<br/>
<tab/><tab/><tab/><tab/>~ addParameter(String, String) -> addFormParam(String, String)<br/>
<tab/><tab/><tab/><tab/>~ addQueryParameter(String, String) -> addQueryParam(String, String)<br/>
<tab/><tab/><tab/><tab/>- setBody(EntityWriter, long)<br/>
<tab/><tab/><tab/><tab/>- setBody(EntityWriter)<br/>
<tab/><tab/><tab/><tab/>~ setParameters(Map&lt;String,Collection&lt;String&gt;&gt;) -> setFormParams(Map&lt;String,List&lt;String&gt;&gt;)<br/>
<tab/><tab/><tab/><tab/>~ setParameters(FluentStringsMap) -> setFormParams(List&lt;Param&gt;)<br/>
<tab/><tab/><tab/><tab/>+ preparePatch(String)<br/>
<tab/><tab/><tab/><tab/>+ prepareTrace(String)<br/>
<tab/><tab/><tab/><tab/>~ preProcessRequest(FilterContext) -> preProcessRequest(FilterContext&lt;T&gt;)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig<br/>
<tab/><tab/><tab/><tab/>~ getMaxTotalConnections() -> getMaxConnections()<br/>
<tab/><tab/><tab/><tab/>~ getMaxTotalConnectionsPerHost() -> getMaxConnectionsPerHost()<br/>
<tab/><tab/><tab/><tab/>~ getConnectionTimeout() -> getConnectTimeout()<br/>
<tab/><tab/><tab/><tab/>~ getWebSocketIdleTimeoutInMs() -> getWebSocketIdleTimeout()<br/>
<tab/><tab/><tab/><tab/>~ getIdleConnectionTimeoutInMs() -> getReadTimeout()<br/>
<tab/><tab/><tab/><tab/>~ getIdleConnectionInPoolTimeoutInMs() -> getPooledConnectionIdleTimeout()<br/>
<tab/><tab/><tab/><tab/>~ getRequestTimeoutInMs() -> getRequestTimeout()<br/>
<tab/><tab/><tab/><tab/>~ isRedirectEnabled() -> isFollowRedirect()<br/>
<tab/><tab/><tab/><tab/>~ getAllowPoolingConnection() -> isAllowedPoolingConnections()<br/>
<tab/><tab/><tab/><tab/>- getKeepAlive()<br/>
<tab/><tab/><tab/><tab/>~ isCompressionEnabled() -> isCompressionEnforced()<br/>
<tab/><tab/><tab/><tab/>- getConnectionsPool()<br/>
<tab/><tab/><tab/><tab/>- getSSLEngineFactory()<br/>
<tab/><tab/><tab/><tab/>- getRequestCompressionLevel()<br/>
<tab/><tab/><tab/><tab/>~ isSslConnectionPoolEnabled() -> isAllowPoolingSslConnections()<br/>
<tab/><tab/><tab/><tab/>- isUseRawUrl()<br/>
<tab/><tab/><tab/><tab/>+ isDisableUrlEncodingForBoundedRequests()<br/>
<tab/><tab/><tab/><tab/>- isRemoveQueryParamOnRedirect()<br/>
<tab/><tab/><tab/><tab/>- isClosed()<br/>
<tab/><tab/><tab/><tab/>- isUseRelativeURIsWithSSLProxies()<br/>
<tab/><tab/><tab/><tab/>~ getMaxConnectionLifeTimeInMs() -> getConnectionTTL()<br/>
<tab/><tab/><tab/><tab/>- getTimeConverter()<br/>
<tab/><tab/><tab/><tab/>+ isAcceptAnyCertificate()<br/>
<tab/><tab/><tab/><tab/>+ getEnabledProtocols()<br/>
<tab/><tab/><tab/><tab/>+ getEnabledCipherSuites()<br/>
<tab/><tab/><tab/><tab/>+ getSslSessionCacheSize()<br/>
<tab/><tab/><tab/><tab/>+ getSslSessionTimeout()<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfig$Builder<br/>
<tab/><tab/><tab/><tab/>~ setMaxTotalConnections(int) -> setMaxConnections(int)<br/>
<tab/><tab/><tab/><tab/>~ setMaxTotalConnectionsPerHost(int) -> setMaxConnectionsPerHost(int)<br/>
<tab/><tab/><tab/><tab/>~ setConnectionTimeout(int) -> setConnectTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setWebSocketIdleTimeoutInMs(int) -> setWebSocketIdleTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setIdleConnectionTimeoutInMs(int) -> setReadTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setIdleConnectionInPoolTimeoutInMs(int) -> setPooledConnectionIdleTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setRequestTimeoutInMs(int) -> setRequestTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setRedirectEnabled(boolean) -> setFollowRedirect(boolean)<br/>
<tab/><tab/><tab/><tab/>~ setAllowPoolingConnection(int) -> setAllowPoolingConnections(int)<br/>
<tab/><tab/><tab/><tab/>~ setMaximumNumberOfRedirects(int) -> setMaxRedirects(int)<br/>
<tab/><tab/><tab/><tab/>- setKeepAlive(boolean)<br/>
<tab/><tab/><tab/><tab/>~ setCompressionEnabled(boolean) -> setCompressionEnforced(boolean)<br/>
<tab/><tab/><tab/><tab/>- setConnectionsPool(ConnectionsPool&lt;?,?&gt;)<br/>
<tab/><tab/><tab/><tab/>- setSSLEngineFactory(SSLEngineFactory)<br/>
<tab/><tab/><tab/><tab/>- getRequestCompressionLevel()<br/>
<tab/><tab/><tab/><tab/>- setRequestCompressionLevel(int)<br/>
<tab/><tab/><tab/><tab/>~ setAllowSslConnectionPoolEnabled() -> setAllowPoolingSslConnections()<br/>
<tab/><tab/><tab/><tab/>- setUseRawUrl(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setDisableUrlEncodingForBoundedRequests(boolean)<br/>
<tab/><tab/><tab/><tab/>- setRemoveQueryParamOnRedirect(boolean)<br/>
<tab/><tab/><tab/><tab/>- setUseRelativeURIsWithSSLProxies(boolean)<br/>
<tab/><tab/><tab/><tab/>~ setMaxConnectionLifeTimeInMs(int) -> setConnectionTTL(int)<br/>
<tab/><tab/><tab/><tab/>- setTimeConverter(TimeConverter)<br/>
<tab/><tab/><tab/><tab/>+ setAcceptAnyCertificate(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setEnabledProtocols(String[])<br/>
<tab/><tab/><tab/><tab/>+ setEnabledCipherSuites(String[])<br/>
<tab/><tab/><tab/><tab/>+ setSslSessionCacheSize(Integer)<br/>
<tab/><tab/><tab/><tab/>+ setSslSessionTimeout(Integer)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfigBean<br/>
<tab/><tab/><tab/><tab/>~ setIdleConnectionTimeoutInMs(int) -> setReadTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setRequestTimeoutInMs(int) -> setRequestTimeout(int)<br/>
<tab/><tab/><tab/><tab/>~ setMaxConnectionLifeTimeInMs(int) -> setConnectionTTL(int)<br/>
<tab/><tab/><tab/><tab/>~ setRedirectEnabled(boolean) -> setFollowRedirect(boolean)<br/>
<tab/><tab/><tab/><tab/>~ setCompressionEnabled(boolean) -> setCompressionEnforced(boolean)<br/>
<tab/><tab/><tab/><tab/>- setSSLEngineFactory(SSLEngineFactory)<br/>
<tab/><tab/><tab/><tab/>- setConnectionsPool(ConnectionsPool&lt;?,?&gt;)<br/>
<tab/><tab/><tab/><tab/>- setRequestCompressionLevel(int)<br/>
<tab/><tab/><tab/><tab/>- setUseRawUrl(boolean)<br/>
<tab/><tab/><tab/><tab/>- setRemoveQueryParamOnRedirect(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setAcceptAnyCertificate(boolean)<br/>
<tab/><tab/><tab/><tab/>+ setSslSessionCacheSize(Integer)<br/>
<tab/><tab/><tab/><tab/>+ setSslSessionTimeout(Integer)<br/>

<tab/><tab/>~ com.ning.http.client.AsyncHttpClientConfigDefaults<br/>
<tab/><tab/>~ com.ning.http.client.AsyncHttpProvider<br/>
<tab/><tab/><tab/><tab/>- prepareResponse(HttpResponseStatus, HttpResponseHeaders, List&lt;HttpResponseBodyPart&gt;)<br/>

<tab/><tab/>~ com.ning.http.client.Body<br/>
<tab/><tab/><tab/><tab/>- close()<br/>

<tab/><tab/>~ com.ning.http.client.BodyConsumer<br/>
<tab/><tab/><tab/><tab/>- close()<br/>

<tab/><tab/>~ com.ning.http.client.ListenableFuture<br/>
<tab/><tab/><tab/><tab/>- content(V)<br/>
<tab/><tab/><tab/><tab/>- getAndSetWriteHeaders(boolean)<br/>
<tab/><tab/><tab/><tab/>- getAndSetWriteBody(boolean)<br/>

<tab/><tab/>+ com.ning.http.client.ListenableFuture$CompletedFailure&lt;T&gt;<br/>

<tab/><tab/>~ com.ning.http.client.websocket.* -> com.ning.http.client.ws.*<br/>
 ** Plus Many, Many more. **

#### 1.9.31 — 2.0.0?




