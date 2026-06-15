# Anthropic

Examples of the request/response bodies for models that have been tested and verified to work. The instrumentation should continue to correctly process new
models as long as they match the model naming prefixes in `llm.models.SupportedModels` and the request/response structure stays the same as the examples listed
here.

## Claude Models

### Text Completion Models

The following models have been tested:

* Claude(`anthropic.claude-v2`, `anthropic.claude-v2:1`)
* Claude Instant(`anthropic.claude-instant-v1`)

#### Sample Request

```json
{
  "stop_sequences": [
    "\n\nHuman:"
  ],
  "max_tokens_to_sample": 1000,
  "temperature": 0.5,
  "prompt": "Human: What is the color of the sky?\n\nAssistant:"
}
```

#### Sample Response

```json
{
  "completion": " The sky appears blue during the day because molecules in the air scatter blue light from the sun more than they scatter red light. The actual color of the sky varies some based on atmospheric conditions, but the primary color we perceive is blue.",
  "stop_reason": "stop_sequence",
  "stop": "\n\nHuman:"
}
```

### Embedding Models

Not supported by Claude.
