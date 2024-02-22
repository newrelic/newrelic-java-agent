This module instruments `TraceOps#txn` call in the newrelic zio 2 scala api.
This is uses instead of the `@Trace` annotation which eagerly starts the Transcaction. This instrumentation takes 
care of the lazy ZIO structure created in ZIO