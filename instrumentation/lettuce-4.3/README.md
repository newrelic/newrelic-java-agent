Lettuce 4.3 Instrumentation
===========================

This instrumentation only supports synchronous and asynchronous API usage.

It does NOT support reactive as this version uses RxJava Observables which at
the time of this documentation, is not supported by the Java Agent. For reactive
Lettuce instrumentation, please use 5 or 6 for reactive support with Reactor Core.