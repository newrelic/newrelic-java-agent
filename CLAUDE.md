# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the New Relic Java Agent, a bytecode instrumentation agent that monitors Java applications at runtime. The agent attaches via the `-javaagent` JVM flag, uses ASM (version 9.9.1) for bytecode manipulation, and employs a "weaver" pattern to insert monitoring code into target classes as they are loaded by the JVM classloader.

The agent captures transactions, traces, metrics, errors, and logs from 440+ supported frameworks and libraries, sending telemetry data to the New Relic platform.

The product optimizes for:
- safety and stability above all else (should never crash the JVM being monitored)
- minimal impact on CPU and memory resources of the JVM being monitored
- backwards compatibility targeting the Java 8 runtime
- collection of application profiling data that is useful and accurate
- clarity over cleverness
- accessible object-oriented design

Avoid over-engineering. If a simpler solution exists, use it.

## Critical Constraints

These rules are non-negotiable:

- **Java 8 language level** for all production code (no newer language features)
- **Do NOT use lambda expressions in weaver instrumentation modules** — the weaver's bytecode rewriting cannot reliably transform lambdas due to `invokedynamic` bootstrap methods and classloader isolation; use anonymous classes instead
- **Do NOT change public APIs** without explicit instructions
- **Do NOT change weaver code** without explicit instructions
- **Preserve backward compatibility** for all shared components
- **Flag major architectural changes** before implementing — don't just do it
- **Instrumentation modules must be self-contained** — dependencies must be shaded
- **New dependencies require shading and licensing** — only add if they provide significant value and always ask for permission first

## Tech Stack

- Gradle for build and dependency management
- JUnit + Mockito for tests
- ASM 9.9.1 for bytecode manipulation
- Caffeine for high-performance caching
- Apache `HttpClient` for data transport to the New Relic backend
- Shadow Gradle plugin for relocating packages of project dependencies
- Spotbugs for static code analysis
- Jacoco for code coverage

## Key File Paths

| Entry Point           | Path                                                                              |
|-----------------------|-----------------------------------------------------------------------------------|
| Agent bootstrap       | `newrelic-agent/src/main/java/com/newrelic/bootstrap/BootstrapAgent.java`         |
| Configuration         | `newrelic-agent/src/main/java/com/newrelic/agent/config/AgentConfigImpl.java`     |
| Service registry      | `newrelic-agent/src/main/java/com/newrelic/agent/service/ServiceManagerImpl.java` |
| Weave engine          | `newrelic-weaver/src/main/java/com/newrelic/weave/ClassWeave.java`                |
| Weave manifest cache  | `gradle/script/cache_weave_attributes.gradle.kts`                                 |
| Code style definition | `dev-tools/code-style/java-agent-code-style.xml`                                  |

## Architecture

The agent uses Java's `Instrumentation` API (JSR 163) with two entry points in `BootstrapAgent`:

```
premain(String agentArgs, Instrumentation inst)   ← normal startup via -javaagent
agentmain(String agentArgs, Instrumentation inst)  ← dynamic attach
```

### Core Modules

| Module                   | Purpose                                                                                                                                                   |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `newrelic-agent`         | Main agent implementation; ServiceManager, configuration, harvest cycle, data transport. Built as a shadow JAR with all dependencies relocated.           |
| `newrelic-weaver`        | Bytecode weaving engine using ASM. Matches weave classes to target classes and applies transformations at class-load time.                                |
| `newrelic-weaver-api`    | Annotations for authoring weave classes (`@Weave`, `@WeaveAllImplementations`, `@NewField`, `@WeaveWithAnnotation`).                                      |
| `newrelic-api`           | Public API for custom instrumentation (`@Trace`, `NewRelic.getAgent()`, custom events/metrics). Ships with no-op implementations.                         |
| `agent-bridge`           | Runtime bridge between instrumentation modules and agent core. Provides `AgentBridge` static facade with volatile references swapped in when agent loads. |
| `agent-bridge-datastore` | Datastore-specific bridge interfaces (connection URL parsing, vendor detection, instance metrics).                                                        |
| `agent-interfaces`       | Internal agent interfaces shared across modules.                                                                                                          |
| `agent-model`            | Shared data models (metric names, error data, custom insight events).                                                                                     |
| `infinite-tracing`       | Infinite Tracing via gRPC streaming to trace observer.                                                                                                    |

### Instrumentation Modules (`instrumentation/`)

443+ modules, each targeting a specific framework/version (e.g., `spring-webmvc-6.0.0`, `kafka-clients-3.6.0`). Each module contains:
- **Weave classes** (`@Weave`) — Code injected into target classes
- **Utility classes** — Helpers available to woven code
- **SkipIfPresent classes** — Conditions for when the module should not apply

Additional modules: Scala support (`newrelic-scala-api`, `newrelic-scala3-api`, Cats/ZIO/Monix integrations), build infrastructure (`buildSrc`, `instrumentation-build`), and test utilities (`functional_test`, `instrumentation-test`).

### Key Architectural Patterns

- **Classloader isolation** — Agent code on bootstrap and `JVMAgentClassLoader` classloaders, instrumentation in separate classloaders. `agent-bridge` classes are visible at runtime to instrumentation but follow a different compile-time dependency path.
- **Manifest-based weave attributes** — Cached in JAR manifests to avoid scanning every class (see `cache_weave_attributes.gradle.kts`)
- **Service lifecycle** — `ServiceManager`/`ServiceFactory` manage all agent services
- **Configuration-driven** — Extensive YAML-based configuration for selective features

## Coding Conventions

- Format code using `./gradlew googleJavaFormat` (verify with `./gradlew verifyGoogleJavaFormat`)
- Use descriptive names; avoid acronyms where possible
- Comments only when intent is non-obvious
- No dead code or commented-out blocks in commits
- Error handling must be explicit — no silent catches
- Error messages must include what happened AND, if possible, what to do next
- **Wide version compatibility** — instrumentation code may look overcomplicated for compatibility reasons; read all comments before changing

## Common Pitfalls

- **Don't `clean` unnecessarily** — The shadow JAR build for `newrelic-agent` is slow. Only use `clean` when switching branches or fixing stale class issues. For iterative development, `./gradlew :module:test` without `clean` is much faster.
- **Instrumentation tests need the agent JAR** — Functional tests require `newrelic-agent/build/newrelicJar/newrelic.jar` to exist. Build it first with `./gradlew jar` if missing.
- **Module-scoped tasks** — Always scope Gradle tasks to the module you're working on (e.g., `./gradlew :newrelic-agent:test`, `./gradlew :instrumentation:spring-webmvc-6.0.0:test`) rather than running repo-wide.
- **Java version for newer modules** — If an instrumentation module targets Java 11+/17+, you must pass `-Ptest11` or `-Ptest17` to gradle or the instrumentation won't apply and tests will fail even though they appear to run.
- **Docker for Testcontainers** — Some instrumentation tests require Docker. If tests fail with connection errors, check Docker is running.

## Testing and Quality

Before considering any task complete:
1. Run `./gradlew :module:verifyGoogleJavaFormat` on modified modules
2. Run `./gradlew :module:googleJavaFormat` to fix formatting if needed
3. Run relevant tests for modified logic

Testing rules:
- Unit tests for all reusable logic
- Empty state, null state, and error state must all be handled

## File Placement

- Feature-specific logic that won't be reused → co-locate with the feature
- Do not create a new abstraction for a one-off use case
- Prefer editing an existing component over creating a near-duplicate

## Commands

### Build

JDK 8 is required (`JAVA_HOME` must point to JDK 8). Configure JDK paths in `~/.gradle/gradle.properties`:
```
jdk8=/path/to/jdk8
jdk17=/path/to/jdk17
```

Verify environment: `./gradlew --version` (should show JVM 1.8.x)

- Build agent jar only: `./gradlew clean jar --parallel`
- Build agent with all checks: `./gradlew clean build --parallel`

**Artifacts:**
- Agent: `newrelic-agent/build/newrelicJar/newrelic.jar`
- API: `newrelic-api/build/libs/newrelic-api-*.jar`

### Code Analysis

- Run Spotbugs: `./gradlew :module:spotbugsMain`

### Tests

#### Unit Tests

- All unit tests: `./gradlew -PnoInstrumentation test --continue --parallel`
- Single module: `./gradlew -PnoInstrumentation :newrelic-weaver:test --parallel`
- Single test: `./gradlew -PnoInstrumentation :newrelic-weaver:test --tests "com.newrelic.weave.LineNumberWeaveTest.testRemoveWeaveLineNumbers" --parallel`
- With specific JDK: add `-Ptest17`
- Include Scala: add `-PincludeScala`

#### Functional Tests

Require the agent JAR to be built first (`./gradlew jar`).

- All: `./gradlew functional_test:test --continue --parallel`
- Single: `./gradlew functional_test:test --tests test.newrelic.test.agent.AgentTest --parallel`

#### Instrumentation Tests

- Single module: `./gradlew :instrumentation:akka-http-core-10.0.11:test --parallel`
- Single test: `./gradlew :instrumentation:vertx-web-3.2.0:test --tests com.nr.vertx.instrumentation.RoutingTest --parallel`
- Scala module: `./gradlew -PincludeScala :instrumentation:sttp-2.13_2.2.3:test --parallel`
- Java 17+ module: `./gradlew -Ptest17 :instrumentation:module-name:test --parallel`