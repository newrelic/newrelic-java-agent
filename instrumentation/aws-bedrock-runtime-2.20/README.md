# AWS Bedrock Runtime Instrumentation

## About

Instruments invocations of LLMs via AWS Bedrock Runtime.

## Support

### Supported Clients/APIs

The following AWS Bedrock Runtime clients and APIs are supported:

* `BedrockRuntimeClient`
  * `invokeModel`
* `BedrockRuntimeAsyncClient`
  * `invokeModel`

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

LLM events also have some unique limits for their attributes...

```
Regardless of which implementation(s) are built, there are consistent changes within the agents and the UX to support AI Monitoring.

Agents should send the entire content; do not truncate it to 256 or 4096 characters

Agents should move known token counts to the LlmChatCompletionMessage

Agents should remove token counts from the LlmChatCompletionSummary
```


call out llm.<user_defined_metadata> behavior

Can be built via `LlmEvent` builder

### Model Invocation/Request/Response

* `ModelInvocation`
* `ModelRequest`
* `ModelResponse`

### Metrics



## Config



## Testing


## TODO
* Add custom `llm.` attributes
* Wire up async client
* Switch instrumentation back to BedrockRuntimeClient/BedrockRuntimeAsyncClient interfaces
* Clean up request/response parsing logic
* Wire up Config
  * Generate `Supportability/{language}/ML/Streaming/Disabled` metric?
* Set up and test new models
* Write instrumentation tests
* Finish readme
