Vert.x 4.00 Core Instrumentation
================================

### HTTP Client

The Vert.x core instrumentation module is mainly concerned with instrumenting the low level Vert.x HTTP Client and associated Future instances.

The instrumentation module can instrument client instances created various ways. For example, via inline callbacks:
```text
        HttpClient httpClient = vertx.createHttpClient();
        httpClient.request(HttpMethod.POST, port,"localhost", "/", reqAsyncResult -> {
            if (reqAsyncResult.succeeded()) {   //Request object successfully created
                HttpClientRequest request = reqAsyncResult.result();
                request.send(respAsyncResult -> {   //Sending the request
                    if (respAsyncResult.succeeded()) {
                        HttpClientResponse response = respAsyncResult.result();
                        response.body(respBufferAsyncResult -> {  //Retrieve response
                            if (respBufferAsyncResult.succeeded()) {
                                // Handle body
                            }
                        });
                    }
                });
            }
        });
```

Or clients created that utilize Futures for response handling:
```text
       HttpClient httpClient = vertx.createHttpClient();
       httpClient.request(HttpMethod.GET, port, "localhost", "/", ar -> {
            if (ar.succeeded()) {
                HttpClientRequest request = ar.result();
                request.send("foo")
                        .onSuccess(response -> {
                            //Success handler
                        }).onFailure(err -> {
                            //Failure handler
                        });
            }
        });
```
