The instrumentation test framework cannot test instrumentation modules that weave core JDK classes. 

In such cases use functional tests instead: `newrelic-java-agent/functional_test/src/test/java/test/newrelic/test/agent/JavaUtilLoggerTest.java`
