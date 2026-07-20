# buildSrc

Gradle build logic for the New Relic Java Agent. Contains custom tasks, Shadow JAR transformers,
and build utilities used across the project.

## DependencyPatcher

`DependencyPatcher` is a Shadow JAR `Transformer` that post-processes shaded dependency classes
during the agent shadow JAR build. It runs on every `.class` file under
`com/newrelic/agent/deps/` — the namespace into which all agent dependencies are relocated.

Because the Shadow plugin allows only one transformer to process a given class, all patching
logic is consolidated here rather than spread across individual transformers.

### How it works

For each candidate class, `DependencyPatcher` runs two ASM passes:

1. **Verification pass** (`canTransformResource`) — each `Patcher` contributes a read-only
   `ClassVisitor`. If any patcher sets `shouldTransform = true`, the class is queued for
   rewriting. This avoids the cost of rewriting classes that need no changes.

2. **Rewriting pass** (`transform`) — each `Patcher` contributes a rewriting `ClassVisitor`
   that is chained together. The chain feeds into a `ClassWriter`, producing the patched
   bytecode which is written to a temp file and later emitted into the output JAR.

### Patcher implementations

| Class                                            | What it does |
|--------------------------------------------------|---|
| `ModifyReferencesToLog4j2Plugins`                | Rewrites string LDC constants (bytecode instructions that load a constant value — in this case a string path — onto the operand stack) that reference the original `Log4j2Plugins.dat` path to the relocated path inside the agent JAR. |
| `RedirectGetLoggerCalls`                         | Redirects `Logger.getLogger(String)` and `Logger.getLogger(String, String)` static calls to `Logger.getGlobal()` to prevent dependency code from creating loggers that conflict with the agent's logging setup. |
| `RemoveUnsupportedAnnotationTargets`<sup>1</sup> | Caffeine v3 introduced a dependency on jspecify, whose `NullMarked` annotation declares `@Target` values that include `ElementType.MODULE` (Java 9) and `ElementType.RECORD_COMPONENT` (Java 16). When the shaded Caffeine v3 classes are present in the agent JAR and Spring scans it, Java 8's `AnnotationParser` encounters these unknown enum constants and throws `ArrayStoreException`. This patcher removes those two values from the `@Target` annotation on the shaded `NullMarked` class at build time. |
| `UnmappedDependencyErrorGenerator`               | Validates that no `com/newrelic/` class references packages outside the allowed set. Throws a build error if an unshaded dependency reference is found. This is always last in the chain and has no rewriting visitor — it exists solely to catch missing shadow relocations at build time. |

`1` - This is safe because Caffeine 3 only utilizes the jspecify annotations for static code analysis tools, not for any sort of runtime reflection.