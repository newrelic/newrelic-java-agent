# Test Resources

This directory contains resources required for a variety of different agent test types.

## Cross Agent Tests

Cross agent tests are shared across all agent teams and are meant to ensure parity with certain features. These tests are maintained in their own [cross_agent_tests](https://source.datanerd.us/agents/cross_agent_tests) repo, they are pulled into this repo as a subtree by running the following git command (this should be run from the top level `newrelic-java-agent` project directory):

```shell
git subtree pull --prefix=newrelic-agent/src/test/resources/com/newrelic/agent/cross_agent_tests git@source.datanerd.us:agents/cross_agent_tests.git master --squash
```

Cross agent tests define test cases and their expected results in JSON format, which the agent will typically parse and execute as unit tests.

## Uncross Agent Tests

These are tests that utilize the same format as the cross agent tests but are specific to the Java agent and thus don't exist in the `cross_agent_tests` repo.

## Miscellaneous

This directory also contains various test resources such as custom instrumentation XML, agent config YAML, extension jars, etc.
