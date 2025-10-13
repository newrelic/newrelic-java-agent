Kotlin Coroutines Suspends Instrumentation
===========================

This instrumentation is used to cover Kotlin Coroutines versions 1.4.0 exclusive

Provides instrumentation for Kotlin Suspend Functions.  It will not track internal Kotlin Coroutine Suspend functions.  Out of the box it will capture any Suspend Function outside of the internal Suspend Functions.   
You can configure the agent to ignore certain suspend functions based on the classname or Continuation name using the actual value or a regular expression to ignore any suspend functions that match it.

## Reporting of Suspend Functions
The execution time of a suspend function will be captured with the metric(span) name that starts with Custom/Kotlin/Coroutines/SuspendFunction/ and a name.  Typically the name will be similar to this: Continuation at *classname*(*filename*.kt:*linenumber*)

## Configuration
The following configuration are based on a Kotlin item added to newrelic.yml.  The settings are dynamic and will change within a minute or so after saving newrelic.yml.

### Suspends Ignores Configuration
To stop tracking a Suspend whose metric name starts with "Custom/ContinuationWrapper/resumeWith/" use the remaining part of the metric name as the configuration or a regular expression that matches it and other Suspends that match, add a continuations item with the suspend functions to ignore.  
Additional you can ignore Suspend functions based on the class name or you can use a regular expression to ignore Suspend functions from a class or package.

**Example**

&nbsp;&nbsp;Kotlin:   
&nbsp;&nbsp;&nbsp;&nbsp;ignores:    
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;suspends: "Continuation at com.nrlabs.AsyncKt.main$task1(async.kt:11)"

#### Notes on Regular Expressions
This extension uses Java regular expression so it is recommended to consult a Java regular expression cheatsheet if you are not familar with it.   



