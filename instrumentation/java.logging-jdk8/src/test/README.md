The instrumentation test framework cannot be used to test instrumentation modules that weave core JDK classes, in such cases we use functional tests instead.  

See tests here: `newrelic-java-agent/functional_test/src/test/java/test/newrelic/test/agent/JavaUtilLoggerTest.java`
