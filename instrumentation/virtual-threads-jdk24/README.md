# virtual-threads-jdk24

Module to instrument Java virtual threads.

### Notes
- This module _only_ supports instrumenting virtual threads when running on Java 24 or greater. This is
because of how pre-Java 24 runtimes pinned virtual threads to its underlying platform thread when
encountering synchronized code blocks. This greatly reduces scalability in applications that
utilize virtual threads that access synchronized code. [JEP 491](https://openjdk.org/jeps/491) 
introduced in Java 24 corrected this.

- This module is disabled by default. To enable the module, add the following to the agent's
newrelic.yml config file, underneath the `common` stanza:
```yaml
  class_transformer:
    com.newrelic.instrumentation.virtual-threads-jdk24:
      enabled: true
```

- This module's per-thread overhead is comparable to any other agent instrumentation that propagates
NR transaction context across threads (modules that wrap `Executor`/`Runnable` work for example). 
However, virtual threads are designed to be created and disposed of in large numbers, and each one
instrumented by this module carries its own tracing overhead. Applications that create a very 
large number of virtual threads can see significant performance overhead as a result. It is 
recommended that any application that creates a large number of virtual threads be tested with the 
agent in a lower environment before enabling in production.

- The metric name for the virtual thread `run` method tracer is `Java/VirtualThread/run`. Any traced 
activity that occurs in the virtual thread will be captured underneath this named segment (database,
external calls, etc).

