# Reactor Netty Instrumentation

Instrumentation for Reactor Netty server and also some widely used Reactor Core library code.

This module is largely responsible for instrumenting the Reactor Core library to facilitate the passing, retrieval,
and linking of `Tokens` across contexts to tie asynchronous threads together for individual `Transactions`.

This instrumentation is dependent on other instrumentation modules to start a `Transaction`.
Typically, the `netty-n.n` modules work with this instrumentation and will start a `Transaction` (see `NettyDispatcher#channelRead`).

Most commonly this instrumentation comes into play with SpringBoot usage, in which case the `spring` and `spring-webflux` 
instrumentation modules also apply and should result in `Transactions` being renamed after the Spring controller.

## Key Components

* `TokenLinkingSubscriber`
    Implementation of a `reactor.core.CoreSubscriber` (a `Context` aware subscriber) that can be added as
    a lifecycle hook on `Flux`/`Mono` operators to propagate, retrieve, and link `Tokens` across async contexts. This is done in various places as follows:

    ```java
    if (!Hooks_Instrumentation.instrumented.getAndSet(true)) {
        Hooks.onEachOperator(TokenLinkingSubscriber.class.getName(), tokenLift());
    }
    ```

* `Schedulers_Instrumentation` and `HttpTrafficHandler_Instrumentation`  
    Both of these classes serve as entry points to add the `TokenLinkingSubscriber` sub-hook.

* Scheduler `Task`s  
    Reactor Core Scheduler tasks that execute on asynchronous threads. These are instrumented as points to link `Tokens`.

## Troubleshooting

In cases where a `Transaction` gets named `/NettyDispatcher` (or named after a security `Filter`) it usually indicates that context was lost somewhere in 
reactor code and that activity on threads where other instrumentation would typically apply could not be linked to the originating `Transaction` thread.
Figuring out how to propagate and link a `Token` across the threads should resolve the issue. 
