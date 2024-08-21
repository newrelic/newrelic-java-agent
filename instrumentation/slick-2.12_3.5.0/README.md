# Slick 3.5.0 Instrumentation

This instrumentation hooks into the `run` Function parameter of `AsyncExecutor.prioritizedRunnable`. 
We create a segment, named `ORM/Slick/SlickQuery`, whenever the `run` Function is eventually called. 

Previous versions of the Slick instrumentation wrapped various executors, execution contexts, and runnables
related to Slick's `AsyncExecutor`. This created casting issues in Slick 3.5.0+, because our wrappers effectively upcasted
these types, which have concrete implementations in the Slick source code. Our wrappers ignored critical overridden methods, 
falling through to the default implementations and disrupting Slick's underlying concurrency mechanism. 

To avoid these casting issues, we are no longer wrapping anything, except the `run` Function. 