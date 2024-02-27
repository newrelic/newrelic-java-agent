# AWS Bedrock Runtime Instrumentation

## About

Instruments invocations of LLMs via the AWS Bedrock Runtime SDK.

## Support

### Supported Clients/APIs

The following AWS Bedrock Runtime clients and APIs are supported:

* `BedrockRuntimeClient`
  * `invokeModel`
* `BedrockRuntimeAsyncClient`
  * `invokeModel`

Note: Currently, `invokeModelWithResponseStream` is not supported.

### Supported Models

Currently, only the following text-based foundation models are supported:

* Anthropic Claude
* Amazon Titan 
* Meta Llama 2 
* Cohere Command 
* AI21 Labs Jurassic

## Involved Pieces

### LLM Events

The main goal of this instrumentation is to generate the following LLM events to drive the UI.

* `LlmEmbedding`: An event that captures data specific to the creation of an embedding.
* `LlmChatCompletionSummary`: An event that captures high-level data about the creation of a chat completion including request, response, and call information.
* `LlmChatCompletionMessage`: An event that corresponds to each message (sent and received) from a chat completion call including those created by the user, assistant, and the system.

These events are custom events sent via the public `recordCustomEvent` API. Currently, they contribute towards the following Custom Insights Events limits (this will likely change in the future). Because of this, it is recommended to increase `custom_insights_events.max_samples_stored` to the maximum value of 100,000 to best avoid sampling issue. LLM events are sent to the `custom_event_data` collector endpoint but the backend will assign them a unique namespace to distinguish them from other custom events.

```yaml
  custom_insights_events:
    max_samples_stored: 100000
```

LLM events also have some unique limits for the content attribute... 

```
Regardless of which implementation(s) are built, there are consistent changes within the agents and the UX to support AI Monitoring.

Agents should send the entire content; do not truncate it to 256 or 4096 characters

Agents should move known token counts to the LlmChatCompletionMessage

Agents should remove token counts from the LlmChatCompletionSummary
```



Can be built via `LlmEvent` builder

### Model Invocation/Request/Response

* `ModelInvocation`
* `ModelRequest`
* `ModelResponse`

### Custom LLM Attributes

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



                // Set llm = true agent attribute required on TransactionEvents


## Config

`ai_monitoring.enabled`: Indicates whether LLM instrumentation will be registered. If this is set to False, no metrics, events, or spans are to be sent.
`ai_monitoring.streaming.enabled`: NOT SUPPORTED

## Testing


## TODO
* Clean up request/response parsing logic
* Wire up Config
  * Generate `Supportability/{language}/ML/Streaming/Disabled` metric?
* Set up and test new models
* Write instrumentation tests
* Finish readme
