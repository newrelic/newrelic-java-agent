# Log4J1 instrumentation

## Regarding ignored tests under Java 21

Log4J1 supposes it is running on Java 1 unless the Java version has a `.` followed by a number different than `1`.
When it runs on Java 1, it does not set MDC entries.

Corretto distribution of Java 21(.0.0) returns "21" when `System.getProperty("java.version")` is invoked.

So when running our tests with Java 21, MDC will not work. Thus our tests will fail.
It is likely that this will be fixed when 21.0.1 is released. At which point we could re-enable the tests.