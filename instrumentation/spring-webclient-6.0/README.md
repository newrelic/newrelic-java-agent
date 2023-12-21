# spring-webclient-6.0

This instrumentation module is a copy of `spring-webclient-5.0`, the only difference being the dependency it is compiled against (spring-webflux:6.x replacing spring-webflux:5.x).

This was necessary because in Webflux 6, `ClientResponse.statusCode` return type has changed to HttpStatusCode, from HttpStatus in Webflux 5.

The instrumentation does not use that class directly, but a line contains `clientResponse.statusCode().value()` which in bytecode gets transformed in:

    39: invokeinterface #52,  1           // InterfaceMethod org/springframework/web/reactive/function/client/ClientResponse.statusCode:()Lorg/springframework/http/HttpStatusCode;
    44: invokeinterface #58,  1           // InterfaceMethod org/springframework/http/HttpStatusCode.value:()I

while the same code in `spring-webclient-5.0` turns into:

    39: invokeinterface #13,  1           // InterfaceMethod org/springframework/web/reactive/function/client/ClientResponse.statusCode:()Lorg/springframework/http/HttpStatus;
    44: invokevirtual #14                 // Method org/springframework/http/HttpStatus.value:()I