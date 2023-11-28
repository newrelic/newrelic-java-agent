Spring Webflux Instrumentation
===============================

This instrumentation assumes that Spring Webflux usage primarily centered 
around maintaining non-blocking flow from the beginning of a request to
the response rendering completion.  If blocking calls occur during the request
handling, it is possible that the agent will lose transaction context and 
transaction naming will not be able to work as intended.  

This is especially likely to happen when using a `ParallelScheduler`
and `@RequestBody` with a parameter that is NOT wrapped in a `Mono` or `Flux`.

For instance:

```java
@PostMapping("/path")
public Mono<String> submit(@RequestBody String body) {
        ...
}
```

When the `@RequestBody` is passed as a parameter in this manner, the transaction
naming might show up as `NettyDispatcher`.  If so, newer versions of this module
have a workaround.  Please upgrade your version of Spring Webflux to 5.1.0+ and
follow workaround instructions in the README.md for that module.
