Kotlin Coroutines 1.4 Instrumentation
===========================

This instrumentation is used to cover Kotlin Coroutines versions 1.4.0 to 1.7.0 (exclusive)

Provides instrumentation for Coroutines and Continuations with the exception of instances internally in Coroutines or is a Suspend function (Covered by different set of instrumentation)

It also provides instrumentation for calls to the Coroutine functions delay, yield, runBlocking, launch, async, withContext and invoke.  

## Configuration
All the following configurations are based on a coroutines item added to newrelic.yml.  The settings are dynamic and will change within a minute or so after saving newrelic.yml.   


### Delay
By default, a call to the delay function will generate a segment metic with a response time equal to the delay argument.  To disable it and not show delay calls add a delayed.enabled to the coroutines item and set it to false.  
    
&nbsp;&nbsp;coroutines:   
&nbsp;&nbsp;&nbsp;&nbsp;delayed:    
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;enabled: false

### Ignores Configuration 
There is the ability to ignore certain Coroutine items that provide little insight to what is happening in the transaction or cause performance issues because the continuation gets invoked frequently (10000+ invocations per minute) and has a fast invocation time (sub-millisecond).   
These are configured in newrelic.yml and placed in a stanza named coroutines.continuation.ignore   You can configure to ignore Continuations or DispatchedTasks.  It can also ignore based on a CoroutineScope.  The setting is a comma separated list and can be the actual value or a reqular expression.   

#### Continuations
To stop tracking a Continuation whose metric name starts with "Custom/ContinuationWrapper/resumeWith/" use the remaining part of the metric name as the configuration or a regular expression that matches it and other Continuations that match, add a continuations stanza with the items to ignore    
    
**Example**   
   
&nbsp;&nbsp;coroutines:   
&nbsp;&nbsp;&nbsp;&nbsp;continuations:      
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ignore: com.nr.example.MyContinuation   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ignoreRegex: com\.nr\.example\.continuations\..*   

#### CoroutineScopes
To stop tracking a call to async or launch based upon either the class name of the CoroutineScope or the coroutine name from its CoroutineContext.  The spans that can be ignored will start with Custom/Builders followed by async or launch.  The spans usually have two related attributes defined "CoroutineScope-Class" and "CoroutineScope-CoroutineName".   Either of the values can be used to ignore those spans by adding a scopes stanza.    

**Example**

&nbsp;&nbsp;coroutines:   
&nbsp;&nbsp;&nbsp;&nbsp;scopes:   
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ignores: MyFastCoroutine

#### DispatchedTasks
To stop tracking a DispatchedTasks (metric name starts with Custom/DispatchedTask),  add dispatched to the ignores stanza   
**Example**

&nbsp;&nbsp;coroutines:   
&nbsp;&nbsp;&nbsp;&nbsp;dispatched:        
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ignores: com.nr.example.MyDispatchedTask    
    
#### Notes on Regular Expressions
This extension uses Java regular expression so it is recommended to consult a Java regular expression cheatsheet if you are not familar with it.   



