# AWS Bedrock Runtime Instrumentation

## About

Instruments invocations of LLMs made by the AWS Bedrock Runtime SDK.

## Support

### Supported Clients/APIs

The following AWS Bedrock Runtime clients and APIs are supported:

* `BedrockRuntimeClient`
  * `invokeModel`
* `BedrockRuntimeAsyncClient`
  * `invokeModel`

Note: Currently, `invokeModelWithResponseStream` is not supported.

### Supported Models

At the time of the instrumentation being published, the following text-based foundation models have been tested and confirmed as supported. As long as the model ID for an invoked LLM model contains one of the prefixes defined in `SupportedModels`, the instrumentation should attempt to process the request/response. However, if the request/response structure significantly changes the processing may fail. See the `README` for each model in `llm.models.*` for more details on each.

* AI21 Labs
  * Jurassic-2 Ultra (`ai21.j2-ultra-v1`)
  * Jurassic-2 Mid (`ai21.j2-mid-v1`)
* Amazon
  * Titan Embeddings G1 - Text (`amazon.titan-embed-text-v1`)
  * Titan Text G1 - Lite (`amazon.titan-text-lite-v1`)
  * Titan Text G1 - Express (`amazon.titan-text-express-v1`)
  * Titan Multimodal Embeddings G1 (`amazon.titan-embed-image-v1`)
* Anthropic
  * Claude (`anthropic.claude-v2`, `anthropic.claude-v2:1`)
  * Claude Instant (`anthropic.claude-instant-v1`)
* Cohere
  * Command (`cohere.command-text-v14`)
  * Command Light (`cohere.command-light-text-v14`)
  * Embed English (`cohere.embed-english-v3`)
  * Embed Multilingual (`cohere.embed-multilingual-v3`)
* Meta
  * Llama 2 Chat 13B (`meta.llama2-13b-chat-v1`)
  * Llama 2 Chat 70B (`meta.llama2-70b-chat-v1`)

## Involved Pieces

### LLM Events

The main goal of this instrumentation is to generate the following LLM events to drive the UI.

* `LlmEmbedding`: An event that captures data specific to the creation of an embedding.
* `LlmChatCompletionSummary`: An event that captures high-level data about the creation of a chat completion including request, response, and call information.
* `LlmChatCompletionMessage`: An event that corresponds to each message (sent and received) from a chat completion call including those created by the user, assistant, and the system.

These events are custom events sent via the public `recordCustomEvent` API. Currently, they contribute towards the following Custom Insights Events limits (this will likely change in the future).

```yaml
  custom_insights_events:
    max_samples_stored: 100000
```

Because of this, it is recommended to increase `custom_insights_events.max_samples_stored` to the maximum value of 100,000 to best avoid sampling issue. LLM events are sent to the `custom_event_data` collector endpoint but the backend will assign them a unique namespace to distinguish them from other custom events.

### Attributes

#### Agent Attributes

An `llm: true` agent attribute will be set on all Transaction events where one of the supported Bedrock methods is invoked within an active transaction.

#### LLM Event Attributes

Attributes on LLM events use the same configuration and size limits as `custom_insights_events` with two notable exceptions being that the following two LLM event attributes will not be truncated at all:
* `content`
* `input`

This is done so that token usage can be calculated on the backend based on the full input and output content.  

#### Custom LLM Attributes

Any custom attributes added by customers using the `addCustomParameters` API that are prefixed with `llm.` will automatically be copied to `LlmEvent`s. For custom attributes added by the `addCustomParameters` API to be added to `LlmEvent`s the API calls must occur before invoking the Bedrock SDK.

One potential custom attribute with special meaning that customers are encouraged to add is `llm.conversation_id`, which has implications in the UI and can be used to group LLM messages into specific conversations.

### Metrics

When in an active transaction a named span/segment for each LLM embedding and chat completion call is created using the following format:

`Llm/{operation_type}/{vendor_name}/{function_name}`

* `operation_type`: `completion` or `embedding`
* `vendor_name`: Name of LLM vendor (ex: `OpenAI`, `Bedrock`)
* `function_name`: Name of instrumented function (ex: `invokeModel`, `create`)

A supportability metric is reported each time an instrumented framework method is invoked. These metrics are detected and parsed by APM Services to support entity tagging in the UI, if a metric isn't reported within the past day the LLM UI will not display in APM. The metric uses the following format:

`Supportability/{language}/ML/{vendor_name}/{vendor_version}`

* `language`: Name of language agent (ex: `Java`)
* `vendor_name`: Name of LLM vendor (ex: `Bedrock`)
* `vendor_version`: Version of instrumented LLM library (ex: `2.20`)

Note: The vendor version isn't obtainable from the AWS Bedrock SDK for Java so the instrumentation version is used instead.

Additionally, a supportability metric is recorded to indicate if streaming is disabled. Streaming is considered disabled if the value of the `ai_monitoring.streaming.enabled` configuration setting is `false`. If streaming is enabled, no supportability metric will be sent. The metric uses the following format:

`Supportability/{language}/ML/Streaming/Disabled`

* `language`: Name of language agent (ex: `Java`)

Note: Streaming is not currently supported.

## Config

`ai_monitoring.enabled`: Provides control over all AI Monitoring functionality. Set as true to enable all AI Monitoring features.
`ai_monitoring.record_content.enabled`: Provides control over whether attributes for the input and output content should be added to LLM events. Set as false to disable attributes for the input and output content.
`ai_monitoring.streaming.enabled`: NOT SUPPORTED

## Related Agent APIs

AI monitoring can be enhanced by using the following agent APIs:
* `recordLlmFeedbackEvent` - Can be used to record an LlmFeedback event to associate user feedback with a specific distributed trace.
* `setLlmTokenCountCallback` - Can be used to register a Callback that provides a token count.
* `addCustomParameter` - Used to add custom attributed to LLM events. See [Custom LLM Attributes](#custom-llm-attributes)

## Known Issues

When using the `BedrockRuntimeAsyncClient`, which returns the response as a `CompletableFuture<InvokeModelResponse>`, the external call to AWS isn't being captured. This is likely deeper instrumentation of the awssdk core classes, perhaps the `software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient` or `software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient`. The external call is actually made by `NettyRequestExecutor(ctx)).execute()`

```java
"http-nio-8081-exec-9@16674" tid=0x56 nid=NA runnable
        java.lang.Thread.State: RUNNABLE
        at software.amazon.awssdk.http.nio.netty.internal.NettyRequestExecutor.execute(NettyRequestExecutor.java:92)
        at software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient.execute(NettyNioAsyncHttpClient.java:123)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.doExecuteHttpRequest(MakeAsyncHttpRequestStage.java:189)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.executeHttpRequest(MakeAsyncHttpRequestStage.java:147)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.lambda$execute$1(MakeAsyncHttpRequestStage.java:99)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage$$Lambda/0x0000000800aefa78.accept(Unknown Source:-1)
        at java.util.concurrent.CompletableFuture.uniAcceptNow(CompletableFuture.java:757)
        at java.util.concurrent.CompletableFuture.uniAcceptStage(CompletableFuture.java:735)
        at java.util.concurrent.CompletableFuture.thenAccept(CompletableFuture.java:2214)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.execute(MakeAsyncHttpRequestStage.java:95)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage.execute(MakeAsyncHttpRequestStage.java:60)
        at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallAttemptMetricCollectionStage.execute(AsyncApiCallAttemptMetricCollectionStage.java:56)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallAttemptMetricCollectionStage.execute(AsyncApiCallAttemptMetricCollectionStage.java:38)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.attemptExecute(AsyncRetryableStage.java:144)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.maybeAttemptExecute(AsyncRetryableStage.java:136)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage$RetryingExecutor.execute(AsyncRetryableStage.java:95)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage.execute(AsyncRetryableStage.java:79)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage.execute(AsyncRetryableStage.java:44)
        at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
        at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage.execute(AsyncExecutionFailureExceptionReportingStage.java:41)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage.execute(AsyncExecutionFailureExceptionReportingStage.java:29)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallTimeoutTrackingStage.execute(AsyncApiCallTimeoutTrackingStage.java:64)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallTimeoutTrackingStage.execute(AsyncApiCallTimeoutTrackingStage.java:36)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallMetricCollectionStage.execute(AsyncApiCallMetricCollectionStage.java:49)
        at software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncApiCallMetricCollectionStage.execute(AsyncApiCallMetricCollectionStage.java:32)
        at software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder$ComposingRequestPipelineStage.execute(RequestPipelineBuilder.java:206)
        at software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient$RequestExecutionBuilderImpl.execute(AmazonAsyncHttpClient.java:190)
        at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.invoke(BaseAsyncClientHandler.java:285)
        at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.doExecute(BaseAsyncClientHandler.java:227)
        at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.lambda$execute$1(BaseAsyncClientHandler.java:82)
        at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler$$Lambda/0x0000000800ab3088.get(Unknown Source:-1)
        at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.measureApiCallSuccess(BaseAsyncClientHandler.java:291)
        at software.amazon.awssdk.core.internal.handler.BaseAsyncClientHandler.execute(BaseAsyncClientHandler.java:75)
        at software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler.execute(AwsAsyncClientHandler.java:52)
        at software.amazon.awssdk.services.bedrockruntime.DefaultBedrockRuntimeAsyncClient.invokeModel(DefaultBedrockRuntimeAsyncClient.java:161)
```


## TODO
* Test env var and sys prop config
* Write instrumentation tests
* Refactor test app to have multiple invokeMethods for a single transaction...
* Figure out how to get external call linked with async client
