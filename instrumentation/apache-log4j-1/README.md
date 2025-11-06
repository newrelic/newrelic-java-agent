# Log4J1 instrumentation

## Regarding ignored tests

Log4J1 supposes it is running on Java 1 unless the Java version has a `.` followed by a number different than `1`.
When it runs on Java 1, it does not set MDC entries.

Tests will fail until at least a version `x.0.1` is released of a JRE.