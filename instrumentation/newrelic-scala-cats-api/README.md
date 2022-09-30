This module instruments `TraceOps#txn` call in the newrelic cats-effect 2 scala api.
This is uses instead of the `@Trace` annotation which eagerly starts the Transcaction. This instrumentation takes 
care of the lazy IO/Async[F] structure created in Cats-Effect 2