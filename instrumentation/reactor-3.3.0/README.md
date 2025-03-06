# Reactor Instrumentation

Instrumentation for Reactor Core library code. This module provides mostly the same functionality of `netty-reactor-0.9.0`, but this will only apply when netty
is not being used.

This instrumentation module is a subset of the `netty-reactor-0.9.0` instrumentation. It does not contain anything related to HTTP nor starting transactions and has added Skips for when `reactor-netty` classes are present.

Changes to `netty-reactor` should be mirrored here and vice-versa.

## Why not separate the functionality?
`netty-reactor` modules register the `tokenLift` in the Hooks class from two different code paths, one from reactor and another from netty. To make sure that
`tokenLift` is registered only once, an AtomicBoolean new field is added to the `Hooks` class. Before registering, that field is checked and if false,
`tokenLift` is registered and the field is set to true.

The code cannot be separated in modules because the new field is not visible across modules, so there would be no way to make sure that `tokenLift` gets
registered only once.

## Notice
This module will only properly link the code if the Mono/Flux is subscribed on a scheduler.

Example:
```
Flux.just(1, 2, 3)
  .map(i -> doSomething(i))
  .subscribeOn(Schedulers.parallel())
  .subscribe();
```
