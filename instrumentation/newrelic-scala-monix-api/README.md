This module instruments `TraceOps#txn` call in the newrelic monix scala api.
This is uses instead of the `@Trace` annotation which eagerly starts the Transcaction. This instrumentation takes 
care of the lazy Task structure created in Monix