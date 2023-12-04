# Reactor Instrumentation

Instrumentation for Reactor Core library code.

This instrumentation module is a subset of the `netty-reactor-0.9.0` instrumentation. It does not contain anything related to HTTP nor starting transactions and has added Skips for when `reactor-netty` classes are present.

The contents of the `netty-reactor` module were not moved to this module because it would cause the `tokenLift` to register twice in the `Hooks` class.  