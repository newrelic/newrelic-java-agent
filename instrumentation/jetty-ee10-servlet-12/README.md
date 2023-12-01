# Jetty 12 EE 10 Servlet Instrumentation

With the introduction of [Jetty 12](https://webtide.com/introducing-jetty-12/), Jetty is now designed with the philosophy of being *Servlet API independent*.
As a result **Jetty** can be used as a core server without any involvement with servlets.
In addition, it now supports every **Servlet API** spec starting from Jakarta EE 8 above.

One of the challenges that resulted from this is that while in theory transactions can
be started in a core server, there is no straightforward way to manage transactions
in async servlets. Jetty 12 offers implementation for each Jakarta EE spec so transactions can instead be
managed through the servlet implementations. As a result the Jetty 12 instrumentation was split into 5 new modules.

## Instrumentation Modules

### jetty-12
This module instruments the core Jetty server that handles tasks that does not require the Servlet API.
That includes but is not limited to:

- Reporting threadpool metrics.
- Reporting the dispatcher name and version as well as the port number for the server.
- Adding outbound CAT headers for outgoing requests. CAT however is deprecated during the time this module is made (i.e. December 2023).

### jetty-ee*-servlet-12
This refers to our current module and any module named based on the same pattern, `jetty-ee*-servlet-12`.
These modules instrument the Servlet API implementations for Jetty.
As a result, the instrumentation works with tasks that require us to work with the Servlet API such as:

- Start and ending web transactions
- Linking transactions to async activity
- Handling errors.