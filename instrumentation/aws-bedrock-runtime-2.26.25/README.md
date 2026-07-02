# AWS Bedrock Runtime Converse API Instrumentation

## About

Instruments invocations of LLMs made by the AWS Bedrock Runtime SDK Converse API.

## Support

### Supported Clients/APIs

The following AWS Bedrock Runtime clients and APIs are supported:

* `BedrockRuntimeClient`
    * `converse`
* `BedrockRuntimeAsyncClient`
    * `converse`
    * `converseStream`

### Supported Models

All models should be supported as the Converse API provides a consistent interface for interacting with them, thus eliminating the need for model-specific implementation logic.

## Involved Pieces

### LLM Events

The main goal of this instrumentation is to generate the following LLM events to drive the UI.

* `LlmEmbedding`: An event that captures data specific to the creation of an embedding.
* `LlmChatCompletionSummary`: An event that captures high-level data about the creation of a chat completion including request, response, and call information.
* `LlmChatCompletionMessage`: An event that corresponds to each message (sent and received) from a chat completion call including those created by the user,
  assistant, and the system.

These events are custom events sent via the public `recordCustomEvent` API. Currently, they contribute towards the following Custom Insights Events limits (this
will likely change in the future).

```yaml
  custom_insights_events:
    max_samples_stored: 100000
```

Because of this, it is recommended to increase `custom_insights_events.max_samples_stored` to the maximum value of 100,000 to best avoid sampling issue. LLM
events are sent to the `custom_event_data` collector endpoint but the backend will assign them a unique namespace to distinguish them from other custom events.

### Attributes

#### Agent Attributes

An `llm: true` agent attribute will be set on all Transaction events where one of the supported Bedrock methods is invoked within an active transaction.

#### LLM Event Attributes

Attributes on LLM events use the same configuration and size limits as `custom_insights_events` with two notable exceptions being that the following two LLM
event attributes will not be truncated at all:

* `content`
* `input`

This is done so that token usage can be calculated on the backend based on the full input and output content.

#### Custom LLM Attributes

Any custom attributes added by customers using the `addCustomParameters` API that are prefixed with `llm.` will automatically be copied to `LlmEvent`s. For
custom attributes added by the `addCustomParameters` API to be added to `LlmEvent`s the API calls must occur before invoking the Bedrock SDK.

One potential custom attribute with special meaning that customers are encouraged to add is `llm.conversation_id`, which has implications in the UI and can be
used to group LLM messages into specific conversations.

### Token Counting

The instrumentation implements a three-tier fallback strategy for token counting:

#### Priority Order

1. **Response Object** (most accurate) - Extracts from the models response when available
2. **User Callback** - using the `setLlmTokenCountCallback` API
3. **Backend Tokenization** - Fallback when no token data is provided

#### Summary Event Usage Attributes

When a model provides complete token usage data, the following attributes are added to `LlmChatCompletionSummary` events:

* `response.usage.prompt_tokens` - Number of tokens in the prompt
* `response.usage.completion_tokens` - Number of tokens in the completion
* `response.usage.total_tokens` - Total tokens used (prompt + completion)

These three attributes are only added when ALL token counts are available. If any are missing, NONE are added, and the system falls back to callback or backend tokenization.

#### Message Event Token Counting

The `token_count` attribute on `LlmChatCompletionMessage` events behaves as follows:

* **When summary has complete usage**: `token_count = 0` (signals backend not to tokenize)
* **When usage incomplete, callback registered**: `token_count = <callback result>`
* **When usage incomplete, no callback**: Attribute omitted (backend will tokenize from `content`)

#### Using the Token Count Callback

The `setLlmTokenCountCallback` API provides a fallback when models don't include token data:

```java
NewRelic.getAgent()
    .getAiMonitoring()
    .setLlmTokenCountCallback((model, content) -> {
        // Your tokenization logic
        return calculateTokens(model, content);
    });
```

This callback is only invoked when the model response doesn't include complete token usage data.

### Metrics

When in an active transaction a named span/segment for each LLM embedding and chat completion call is created using the following format:

`Llm/{operation_type}/{vendor_name}/{function_name}`

* `operation_type`: `completion`
* `vendor_name`: Name of LLM vendor (ex: `Bedrock`)
* `function_name`: Name of instrumented function (ex: `converse`)

A supportability metric is reported each time an instrumented framework method is invoked. These metrics are detected and parsed by APM Services to support
entity tagging in the UI, if a metric isn't reported within the past day the LLM UI will not display in APM. The metric uses the following format:

`Supportability/{language}/ML/{vendor_name}/{vendor_version}`

* `language`: Name of language agent (ex: `Java`)
* `vendor_name`: Name of LLM vendor (ex: `Bedrock`)
* `vendor_version`: Version of instrumented LLM library (ex: `2.26.25`)

Note: The vendor version isn't obtainable from the AWS Bedrock SDK for Java so the instrumentation version is used instead.

Additionally, the following supportability metrics are recorded to indicate the agent config state.

```
Supportability/Java/ML/Enabled
Supportability/Java/ML/Disabled

Supportability/Java/ML/Streaming/Enabled
Supportability/Java/ML/Streaming/Disabled

Supportability/Java/ML/RecordContent/Enabled
Supportability/Java/ML/RecordContent/Disabled
```

## Config

### Yaml

`ai_monitoring.enabled`: Provides control over all AI Monitoring functionality. Set as true to enable all AI Monitoring features.  
`ai_monitoring.record_content.enabled`: Provides control over whether attributes for the input and output content should be added to LLM events. Set as false to
disable attributes for the input and output content.  
`ai_monitoring.streaming.enabled`: Enable support for Converse stream API.    

### Environment Variable

```
NEW_RELIC_AI_MONITORING_ENABLED
NEW_RELIC_AI_MONITORING_RECORD_CONTENT_ENABLED
NEW_RELIC_AI_MONITORING_STREAMING_ENABLED
```

### System Property

```
-Dnewrelic.config.ai_monitoring.enabled
-Dnewrelic.config.ai_monitoring.record_content.enabled
-Dnewrelic.config.ai_monitoring.streaming.enabled
```

## Related Agent APIs

AI monitoring can be enhanced by using the following agent APIs:

* `recordLlmFeedbackEvent` - Can be used to record an LlmFeedback event to associate user feedback with a specific distributed trace.
* `setLlmTokenCountCallback` - Can be used to register a Callback that provides a token count.
* `addCustomParameter` - Used to add custom attributed to LLM events. See [Custom LLM Attributes](#custom-llm-attributes)

## Known Issues

When using the `BedrockRuntimeAsyncClient`, which returns the response as a `CompletableFuture<ConverseResponse>`, the external call to AWS isn't being
captured. This is likely to require deeper instrumentation of the AWS SDK core classes, perhaps the `software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient`
or `software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient`. The external call is actually made by `NettyRequestExecutor(ctx).execute()` in `NettyNioAsyncHttpClient`.

```java
"main@1" prio=5 tid=0x3 nid=NA runnable
  java.lang.Thread.State: RUNNABLE
	at software.amazon.awssdk.http.nio.netty.internal.NettyRequestExecutor.execute(NettyRequestExecutor.java:95)
	at software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient.execute(NettyNioAsyncHttpClient.java:136)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.doExecuteHttpRequest(MakeAsyncHttpRequestStage.java:204)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.executeHttpRequest(MakeAsyncHttpRequestStage.java:151)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.lambda$execute$1(MakeAsyncHttpRequestStage.java:104)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage$$Lambda/0x000007fe015c0240.accept(Unknown Source:-1)
	at java.util.concurrent.CompletableFuture.uniAcceptNow(CompletableFuture.java:778)
	at java.util.concurrent.CompletableFuture.uniAcceptStage(CompletableFuture.java:756)
	at java.util.concurrent.CompletableFuture.thenAccept(CompletableFuture.java:2241)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.execute(MakeAsyncHttpRequestStage.java:100)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.execute(MakeAsyncHttpRequestStage.java:65)
	at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallAttemptMetricCollectionStage.execute(AsyncApiCallAttemptMetricCollectionStage.java:62)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallAttemptMetricCollectionStage.execute(AsyncApiCallAttemptMetricCollectionStage.java:41)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.attemptExecute(AsyncRetryableStage.java:102)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.attemptFirstExecute(AsyncRetryableStage.java:89)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.execute(AsyncRetryableStage.java:79)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage.execute(AsyncRetryableStage.java:62)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage.execute(AsyncRetryableStage.java:41)
	at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
	at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage.execute(AsyncExecutionFailureExceptionReportingStage.java:41)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage.execute(AsyncExecutionFailureExceptionReportingStage.java:29)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallTimeoutTrackingStage.execute(AsyncApiCallTimeoutTrackingStage.java:64)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallTimeoutTrackingStage.execute(AsyncApiCallTimeoutTrackingStage.java:36)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallMetricCollectionStage.execute(AsyncApiCallMetricCollectionStage.java:49)
	at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallMetricCollectionStage.execute(AsyncApiCallMetricCollectionStage.java:32)
	at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
	at software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient$RequestExecutionBuilderImpl.execute(AmazonAsyncHttpClient.java:216)
	at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.invoke(BaseAsyncClientHandler.java:288)
	at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.doExecute(BaseAsyncClientHandler.java:227)
	at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.lambda$execute$1(BaseAsyncClientHandler.java:80)
	at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler$$Lambda/0x000007fe0152caf0.get(Unknown Source:-1)
	at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.measureApiCallSuccess(BaseAsyncClientHandler.java:294)
	at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.execute(BaseAsyncClientHandler.java:73)
	at software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler.execute(AwsAsyncClientHandler.java:49)
	at software.amazon.awssdk.services.bedrockruntime.DefaultBedrockRuntimeAsyncClient.converse(DefaultBedrockRuntimeAsyncClient.java:456)
	at software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient.converse(BedrockRuntimeAsyncClient.java:413)
```
