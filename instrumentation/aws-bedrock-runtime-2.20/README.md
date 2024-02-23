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

The main goal of this instrumentation is to generate the following LLM events to drive the UI. These events are custom events sent via the public `recordCustomEvent` API.

* `LlmEmbedding`
* `LlmChatCompletionSummary`
* `LlmChatCompletionMessage`

Currently, they contribute towards the following Custom Insights Events limits (this will likely change in the future). Because of this it is recommended to increase `custom_insights_events.max_samples_stored` to the maximum value of 100,000 to best avoid sampling issue.

```yaml
  custom_insights_events:
    max_samples_stored: 100000
```

LLM events also have some unique limits for their attributes...

they are also bucketed into a unique namespace separate from other custom events on the backend...

```
Regardless of which implementation(s) are built, there are consistent changes within the agents and the UX to support AI Monitoring.

Agents should send the entire content; do not truncate it to 256 or 4096 characters

Agents should move known token counts to the LlmChatCompletionMessage

Agents should remove token counts from the LlmChatCompletionSummary
```


call out llm.<user_defined_metadata> behavior



### Model Invocation/Request/Response

* `ModelInvocation`
* `ModelRequest`
* `ModelResponse`

### Metrics



## Config



## Testing

