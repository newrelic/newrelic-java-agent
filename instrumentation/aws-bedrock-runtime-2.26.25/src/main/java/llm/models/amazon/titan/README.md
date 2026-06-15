# Amazon

Examples of the request/response bodies for models that have been tested and verified to work. The instrumentation should continue to correctly process new
models as long as they match the model naming prefixes in `llm.models.SupportedModels` and the request/response structure stays the same as the examples listed
here.

## Titan Models

### Text Completion Models

The following models have been tested:

* Titan Text G1-Lite (`amazon.titan-text-lite-v1`)
* Titan Text G1-Express (`amazon.titan-text-express-v1`)

#### Sample Request

```json
{
  "inputText": "What is the color of the sky?",
  "textGenerationConfig": {
    "maxTokenCount": 1000,
    "stopSequences": [
      "User:"
    ],
    "temperature": 0.5,
    "topP": 0.9
  }
}
```

#### Sample Response

```json
{
  "inputTextTokenCount": 8,
  "results": [
    {
      "tokenCount": 39,
      "outputText": "\nThe color of the sky depends on the time of day, weather conditions, and location. It can range from blue to gray, depending on the presence of clouds and pollutants in the air.",
      "completionReason": "FINISH"
    }
  ]
}
```

### Embedding Models

The following models have been tested:

* Titan Embeddings G1-Text (`amazon.titan-embed-text-v1`)
* Titan Multimodal Embeddings G1 (`amazon.titan-embed-image-v1`)

#### Sample Request

```json
{
  "inputText": "What is the color of the sky?"
}
```

#### Sample Response

```json
{
  "embedding": [
    0.328125,
    ...,
    0.44335938
  ],
  "inputTextTokenCount": 8
}
```

