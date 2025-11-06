# Jetty 12 Core Server Instrumentation

With the introduction of [Jetty 12](https://webtide.com/introducing-jetty-12/), Jetty is now designed with the philosophy of being *Servlet API independent*.
As a result **Jetty** can be used as a core server without any involvement with servlets. 
In addition, it now supports every **Servlet API** spec starting from Jakarta EE 8 above.

This means this instrumentation module does not have means to manage servlet based tasks such as handling async servlet operations.

As a result this instrumentation module handles the following tasks:

- Reporting threadpool metrics
- Reporting the dispatcher name and version as well as the port number for the server.
- Adding outbound CAT headers for outgoing requests. CAT however is deprecated during the time this module is made (i.e. December 2023).
- Reporting errors
- Starting and ending web transactions