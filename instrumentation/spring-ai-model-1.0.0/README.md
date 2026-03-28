# Spring AI Embedding Model Instrumentation

## About

Instruments invocations of LLMs made by the SpringAI `EmbeddingModel`.

## Requirements

This only works with Java 17+

## Support

### Supported Clients/APIs

TODO

### Supported Models

TODO

## Involved Pieces

### LLM Events

The main goal of this instrumentation is to generate the following LLM events to drive the UI.

* `LlmEmbedding`: An event that captures data specific to the creation of an embedding.

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

An `llm: true` agent attribute will be set on all Transaction events where one of the supported SpringAI `EmbeddingModel` methods is invoked within an active
transaction.

#### LLM Event Attributes

Attributes on LLM events use the same configuration and size limits as `custom_insights_events` with two notable exceptions being that the following two LLM
event attributes will not be truncated at all:

* `content`
* `input`

This is done so that token usage can be calculated on the backend based on the full input and output content.

#### Custom LLM Attributes

Any custom attributes added by customers using the `addCustomParameters` API that are prefixed with `llm.` will automatically be copied to `LlmEvent`s. For
custom attributes added by the `addCustomParameters` API to be added to `LlmEvent`s the API calls must occur before invoking the SpringAI `EmbeddingModel`.

One potential custom attribute with special meaning that customers are encouraged to add is `llm.conversation_id`, which has implications in the UI and can be
used to group LLM messages into specific conversations.

### Metrics

When in an active transaction a named span/segment for each LLM embedding and chat completion call is created using the following format:

`Llm/{operation_type}/{vendor_name}/{function_name}`

* `operation_type`: `completion` or `embedding`
* `vendor_name`: Name of LLM vendor (ex: `SpringAI`)
* `function_name`: Name of instrumented function (ex: `call`, `stream`)

A supportability metric is reported each time an instrumented framework method is invoked. These metrics are detected and parsed by APM Services to support
entity tagging in the UI, if a metric isn't reported within the past day the LLM UI will not display in APM. The metric uses the following format:

`Supportability/{language}/ML/{vendor_name}/{vendor_version}`

* `language`: Name of language agent (ex: `Java`)
* `vendor_name`: Name of LLM vendor (ex: `SpringAI`)
* `vendor_version`: Version of instrumented LLM library (ex: `1.0.0`)

Note: The vendor version isn't obtainable from the SpringAI `EmbeddingModel` so the instrumentation version is used instead.

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
`ai_monitoring.streaming.enabled`: NOT SUPPORTED

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

