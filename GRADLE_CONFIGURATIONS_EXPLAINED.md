# Gradle Configurations Explained

## What is a Configuration?

A **configuration** in Gradle is essentially a **named bucket of dependencies**. Think of it as a labeled container that holds JAR files and their metadata.

```
Configuration = Named set of dependencies that serve a specific purpose
```

---

## Why Do Configurations Exist?

Different parts of your build need different dependencies at different times:

- **Compiling code** needs some dependencies
- **Running tests** needs different (or additional) dependencies
- **Runtime execution** might need different dependencies than compile-time
- **Packaging** might need to include specific dependencies

Configurations let you organize these different dependency sets.

---

## Simple Example

```gradle
configurations {
    myLibraries      // A bucket to hold library JARs
    myTestLibraries  // A bucket to hold test-only JARs
}

dependencies {
    myLibraries 'com.google.guava:guava:30.1.1'
    myTestLibraries 'junit:junit:4.12'
}
```

Now you have:
- `myLibraries` configuration containing Guava
- `myTestLibraries` configuration containing JUnit

---

## Common Built-in Configurations (Java Plugin)

When you apply `plugin: 'java'`, Gradle creates several standard configurations:

| Configuration | Purpose | When Used |
|--------------|---------|-----------|
| `implementation` | Dependencies needed to compile and run | Compile + Runtime |
| `compileOnly` | Dependencies needed only at compile-time | Compile only (e.g., annotations) |
| `runtimeOnly` | Dependencies needed only at runtime | Runtime only (e.g., JDBC drivers) |
| `testImplementation` | Dependencies for compiling and running tests | Test compile + Test runtime |
| `testCompileOnly` | Dependencies only for compiling tests | Test compile only |
| `testRuntimeOnly` | Dependencies only for running tests | Test runtime only |

### Example with Built-in Configurations

```gradle
dependencies {
    // Needed to compile and run the app
    implementation 'com.google.guava:guava:30.1.1'

    // Only needed at compile time (not packaged)
    compileOnly 'javax.servlet:javax.servlet-api:3.0.1'

    // Only needed at runtime (database driver)
    runtimeOnly 'mysql:mysql-connector-java:8.0.28'

    // Only needed for tests
    testImplementation 'junit:junit:4.12'
}
```

---

## How Configurations Work: Resolution

When you reference a configuration in a task, Gradle **resolves** it:

1. **Looks up** all dependencies declared in that configuration
2. **Downloads** the JARs from repositories (Maven Central, etc.)
3. **Resolves transitive dependencies** (dependencies of dependencies)
4. **Returns** a set of files (usually JAR files)

### Example: Using a Configuration in a Task

```gradle
configurations {
    myLibraries
}

dependencies {
    myLibraries 'com.google.guava:guava:30.1.1'
}

task showLibraries {
    doLast {
        // Resolve the configuration to get actual files
        configurations.myLibraries.each { file ->
            println file.name
        }
    }
}
```

Output:
```
guava-30.1.1.jar
failureaccess-1.0.1.jar
listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar
jsr305-3.0.2.jar
checker-qual-3.8.0.jar
error_prone_annotations-2.5.1.jar
j2objc-annotations-1.3.jar
```

Notice it includes Guava's transitive dependencies!

---

## Configuration Inheritance: `extendsFrom`

Configurations can **inherit** from other configurations:

```gradle
configurations {
    myLibraries
    myExtendedLibraries {
        extendsFrom myLibraries  // Inherits all deps from myLibraries
    }
}

dependencies {
    myLibraries 'com.google.guava:guava:30.1.1'
    myExtendedLibraries 'junit:junit:4.12'
}
```

Now `myExtendedLibraries` contains:
- Guava (inherited from `myLibraries`)
- JUnit (declared directly)

### Real Example from Your Code

```gradle
configurations {
    shadowIntoJar
    implementation
}

configurations.implementation.extendsFrom(configurations.shadowIntoJar)

dependencies {
    shadowIntoJar 'com.google.guava:guava:30.1.1'
}
```

This means:
1. `shadowIntoJar` contains Guava
2. `implementation` **also** contains Guava (because it extends from `shadowIntoJar`)
3. Your code can compile against Guava (since it's in `implementation`)
4. Your shadow task can shade Guava (since it's in `shadowIntoJar`)

---

## Practical Use Cases

### Use Case 1: Shading Dependencies

```gradle
configurations {
    shadowIntoJar  // Dependencies to shade/relocate
}

dependencies {
    shadowIntoJar 'com.google.guava:guava:30.1.1'
}

task shadowJar(type: ShadowJar) {
    configurations = [project.configurations.shadowIntoJar]
    relocate 'com.google', 'my.shaded.com.google'
}
```

### Use Case 2: Multiple Versions (Your Dual-Caffeine Case)

```gradle
configurations {
    caffeine2  // Bucket for Caffeine 2.9.3
    caffeine3  // Bucket for Caffeine 3.2.3
}

dependencies {
    caffeine2 'com.github.ben-manes.caffeine:caffeine:2.9.3'
    caffeine3 'com.github.ben-manes.caffeine:caffeine:3.2.3'
}

// Now you can process them separately in different tasks
task shadeCaffeine2(type: ShadowJar) {
    configurations = [project.configurations.caffeine2]
    // Shade 2.9.3 to one package
}

task shadeCaffeine3(type: ShadowJar) {
    configurations = [project.configurations.caffeine3]
    // Shade 3.2.3 to a different package
}
```

### Use Case 3: Separate Test Dependencies

```gradle
configurations {
    unitTestLibraries
    integrationTestLibraries
}

dependencies {
    unitTestLibraries 'junit:junit:4.12'
    integrationTestLibraries 'org.testcontainers:testcontainers:1.17.0'
}

task unitTest(type: Test) {
    classpath = configurations.unitTestLibraries + sourceSets.test.output
}

task integrationTest(type: Test) {
    classpath = configurations.integrationTestLibraries + sourceSets.test.output
}
```

---

## Advanced: Configuration Attributes

In modern Gradle (3.4+), configurations can have attributes to control their behavior:

### `canBeConsumed`

Controls whether **other projects** can depend on this configuration.

```gradle
configurations {
    myApi {
        canBeConsumed = true   // Other projects CAN depend on this
    }
    myInternal {
        canBeConsumed = false  // Other projects CANNOT depend on this
    }
}
```

**When to use `false`:** When a configuration is purely internal to your project.

### `canBeResolved`

Controls whether **Gradle can download** the dependencies.

```gradle
configurations {
    myDependencies {
        canBeResolved = true   // Gradle WILL download these JARs
    }
    myApi {
        canBeResolved = false  // Just declares what you provide, don't download
    }
}
```

**When to use `false`:** When defining what your project provides to others (like an API contract).

### `transitive`

Controls whether to include **dependencies of dependencies**.

```gradle
configurations {
    myLibraries {
        transitive = true   // Include Guava's dependencies too
    }
    myDirectOnly {
        transitive = false  // Only include what I explicitly declare
    }
}

dependencies {
    myLibraries 'com.google.guava:guava:30.1.1'
    // ^ Will also download failureaccess, jsr305, etc.

    myDirectOnly 'com.google.guava:guava:30.1.1'
    // ^ Will ONLY download guava-30.1.1.jar
}
```

---

## Configuration Lifecycle

```
1. Declaration
   configurations { myConfig }

2. Population
   dependencies { myConfig 'group:artifact:version' }

3. Resolution (when needed by a task)
   Gradle downloads JARs from repositories

4. Usage
   Task reads files from configuration
```

---

## Real-World Analogy

Think of configurations like **shipping containers** at a dock:

- **Configuration = Container** with a label (e.g., "Electronics", "Books")
- **Dependencies = Items** you put in the container
- **Resolution = Opening** the container to see what's inside
- **extendsFrom = Combined container** that includes items from multiple containers
- **Tasks = Workers** who use specific containers to do their job

Example:
```gradle
configurations {
    buildTools        // "Tools" container
    runtime           // "Production" container
}

dependencies {
    buildTools 'hammer:hammer:1.0'
    buildTools 'wrench:wrench:2.0'
    runtime 'engine:engine:3.0'
}

// Build task uses "Tools" container
task build {
    doLast {
        configurations.buildTools.each { tool ->
            println "Using tool: ${tool.name}"
        }
    }
}

// Deploy task uses "Production" container
task deploy {
    doLast {
        configurations.runtime.each { component ->
            println "Deploying: ${component.name}"
        }
    }
}
```

---

## Summary Table

| Concept | Purpose | Example |
|---------|---------|---------|
| **Configuration** | Named bucket of dependencies | `configurations { myLibs }` |
| **Dependency** | Library to put in a configuration | `dependencies { myLibs 'com.foo:bar:1.0' }` |
| **Resolution** | Process of downloading JARs | `configurations.myLibs.each { ... }` |
| **extendsFrom** | Inherit deps from another config | `myConfig.extendsFrom(otherConfig)` |
| **canBeConsumed** | Can others depend on this? | `canBeConsumed = false` (internal only) |
| **canBeResolved** | Can Gradle download deps? | `canBeResolved = true` (yes, download) |
| **transitive** | Include transitive dependencies? | `transitive = true` (include deps of deps) |

---

## Key Takeaways

1. **Configurations are buckets** that organize dependencies by purpose
2. **Different phases need different dependencies** (compile vs runtime vs test)
3. **Configurations can inherit** from each other using `extendsFrom`
4. **Most projects use simple configurations** - advanced attributes are optional
5. **Tasks consume configurations** to get the files they need
6. **Your dual-Caffeine setup** uses two separate configurations to hold two different versions of the same library

---

## How This Applies to Your Dual-Caffeine Setup

```gradle
configurations {
    caffeine2  // Bucket for version 2.9.3
    caffeine3  // Bucket for version 3.2.3
}

dependencies {
    caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
    caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')
}

// Task 1: Process version 2.9.3 from caffeine2 bucket
task shadeCaffeine2Jar(type: ShadowJar) {
    configurations = [project.configurations.caffeine2]
    // Shades ONLY 2.9.3
}

// Task 2: Process version 3.2.3 from caffeine3 bucket
task shadeCaffeine3Jar(type: ShadowJar) {
    configurations = [project.configurations.caffeine3]
    // Shades ONLY 3.2.3
}
```

By using **separate configurations**, you can:
- Keep both versions separate
- Process them independently
- Shade them to different packages
- Avoid version conflicts

---

## Further Reading

- [Gradle Docs: Dependency Management](https://docs.gradle.org/current/userguide/dependency_management.html)
- [Gradle Docs: Declaring Configurations](https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:what-are-dependency-configurations)
- [Understanding Configuration Attributes](https://docs.gradle.org/current/userguide/variant_model.html)