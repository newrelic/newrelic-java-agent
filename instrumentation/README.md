## Instrumentation modules

This gradle project contains subprojects for each individual instrumentation module. Instrumentation modules are typically packaged as simple jar files.

### Introduction

Instrumentation modules may contain three types of classes.

1. *Weave classes*. Weave classes are required. Weave classes are usually annotated with [`@Weave`](../newrelic-weaver-api/src/main/java/com/newrelic/api/agent/weaver/Weave.java) or [`@WeaveWithAnnotation`](../newrelic-weaver-api/src/main/java/com/newrelic/api/agent/weaver/WeaveWithAnnotation.java). At runtime, the weaver will extract the bytecode from these classes and insert it into the target class. Although the previous style was to put the weave class in the same package and class name, newer patterns use the class name suffixed with "_Instrumentation" and the `originalName` attribute on the annotation.
1. *Utility classes*. Utility classes are common, but optional. These classes are usually not annotated at all. When an instrumentation module is applied, utility classes are made available to the woven code.
1. *SkipIfPresent classes*. SkipIfPresent classes are optional. These classes are annotated with [`@SkipIfPresent`](../newrelic-weaver-api/src/main/java/com/newrelic/api/agent/weaver/SkipIfPresent.java) and are empty. They are used to narrow the focus of a given instrumentation module. For example, imagine a module successfully applies to versions 1, 2, and 3 of a given application server, but it won't actually function correctly on version 3. Module authors will look for a class that may have been introduced in version 3 and annotate it with `@SkipIfPresent`. If the weaver finds a `@SkipIfPresent` class on the classpath, the instrumentation module will not be applied.

### Expectations 

Instrumentation modules can use any dependencies they need for compilation or testing. However, there are some expectations that the agent puts on instrumentation modules.

1. Instrumentation modules cannot use lambdas. Instead, use anonymous or inner classes. At runtime, utility classes (those without the `@Weave` annotation) are loaded directly onto the classpath of the classloader. The weaver does not support the required rewrite of the `invokedynamic` instruction, nor the relocation of the corresponding class.
1. All required classes must either be already present on the classpath, or they must be present in the instrumentation module jar. The weaver does not provide any POM-like dependency resolution, so the instrumentation module must be wholly contained. Module authors may assume that the New Relic API classes are available (see [newrelic-api](../newrelic-api/src/main/java)). The agent will always inject New Relic API classes on the bootstrap classloader. 

### Cached Weave Attributes

On JVM startup, the Java agent looks at all the available instrumentation modules and determines the classes and methods that would be required for weaving. Normally, it would have to load every class in the instrumentation module to figure out what's required. As a speed optimization, existing instrumentation modules ship with all the necessary information stored as attributes in the instrumentation module's manifest. Then the agent can get the same information from a single text file in the jar.

In a Java-only, Scala-only, or Java+Scala instrumentation module, these attributes are calculated automatically and added to the jar. A task is added to the graph by [cache_weave_attributes.gradle.kts](../gradle/script/cache_weave_attributes.gradle.kts). The task uses the weaver to calculate the attributes and store them in a temporary manifest. This temporary manifest is later added to the manifest by the `jar` task. 

In rare cases, an instrumentation module needs 3rd-party dependencies. _This should generally be avoided._ If it cannot be avoided, you should shadow those dependencies. In an instrumentation module that uses shadow, there are some additional steps that need to be taken.

1. Don't `apply` the shadow plugin. Instead, create the task manually. Otherwise, shadow will attempt to add the weave attributes to the `shadowJar` task, creating a circular dependency.
   ```groovy
   tasks.create("shadowJar", ShadowJar) {
       archiveClassifier.set("shadowed")
       // add additional configuration here
   }
   ```
1. Make the `writeCachedWeaveAttributes` task dependent on your task that generates the shadow jar. Usually this looks like this:
   ```groovy
   project.tasks.getByName("writeCachedWeaveAttributes").dependsOn(shadowJar)
   ```
1. Add the shadow jar artifact to the `instrumentationWithDependencies` configuration:
   ```groovy
   artifacts {
       instrumentationWithDependencies shadowJar
   }
   ```
