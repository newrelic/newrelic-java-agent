# Jetty 12 Servlet Instrumentation

With the introduction of [Jetty 12](https://webtide.com/introducing-jetty-12/), Jetty is now designed with the philosophy of being *Servlet API independent*.
As a result **Jetty** can be used as a core server without any involvement with servlets. 
In addition, it now supports every **Servlet API** spec starting from Jakarta EE 8 above.

Much of the instrumentation work is handled in the core Jetty module. This module handles servlet specific work such as:

- Marking the transaction as a servlet based call with a `requestDispatcher` segment
- Suspending, resuming, and ending transactions associated with async servlet operations
- Reporting errors