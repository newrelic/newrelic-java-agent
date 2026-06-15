# Cohere

Examples of the request/response bodies for models that have been tested and verified to work. The instrumentation should continue to correctly process new
models as long as they match the model naming prefixes in `llm.models.SupportedModels` and the request/response structure stays the same as the examples listed
here.

## Command Models

### Text Completion Models

The following models have been tested:

* Command(`cohere.command-text-v14`)
* Command Light(`cohere.command-light-text-v14`)

#### Sample Request

```json
{
    "p": 0.9,
    "stop_sequences": [
        "User:"
    ],
    "truncate": "END",
    "max_tokens": 1000,
    "stream": false,
    "temperature": 0.5,
    "k": 0,
    "return_likelihoods": "NONE",
    "prompt": "What is the color of the sky?"
}
```

#### Sample Response

```json
{
    "generations": [
        {
            "finish_reason": "COMPLETE",
            "id": "f5700a48-0730-49f1-9756-227a993963aa",
            "text": " The color of the sky can vary depending on the time of day, weather conditions, and location. In general, the color of the sky is a pale blue. During the day, the sky can appear to be a lighter shade of blue, while at night, it may appear to be a darker shade of blue or even black. The color of the sky can also be affected by the presence of clouds, which can appear as white, grey, or even pink or red in the morning or evening light. \n\nIt is important to note that the color of the sky is not a static or fixed color, but rather a dynamic and ever-changing one, which can be influenced by a variety of factors."
        }
    ],
    "id": "c548295f-9064-49c5-a05f-c754e4c5c9f8",
    "prompt": "What is the color of the sky?"
}
```

### Embedding Models

The following models have been tested:

* Embed English(`cohere.embed-english-v3`)
* Embed Multilingual(`cohere.embed-multilingual-v3`)

#### Sample Request

```json
{
    "texts": [
        "What is the color of the sky?"
    ],
    "truncate": "NONE",
    "input_type": "search_document"
}
```

#### Sample Response

```json
{
  "embeddings": [
    [
      -0.002828598,
      ...,
      0.00541687
    ]
  ],
  "id": "e1e969ba-d526-4c76-aa92-a8a705288f6d",
  "response_type": "embeddings_floats",
  "texts": [
    "what is the color of the sky?"
  ]
}
```
