# Meta

Examples of the request/response bodies for models that have been tested and verified to work. The instrumentation should continue to correctly process new
models as long as they match the model naming prefixes in `llm.models.SupportedModels` and the request/response structure stays the same as the examples listed
here.

## Llama 2 Models

### Text Completion Models

The following models have been tested:

* Llama 2Chat 13B (`meta.llama2-13b-chat-v1`)
* Llama 2Chat 70B (`meta.llama2-70b-chat-v1`)

#### Sample Request

```json
{
  "top_p": 0.9,
  "max_gen_len": 1000,
  "temperature": 0.5,
  "prompt": "What is the color of the sky?"
}
```

#### Sample Response

```json
{
  "generation": "\n\nThe color of the sky can vary depending on the time of day and atmospheric conditions. During the daytime, the sky typically appears blue, which is caused by a phenomenon called Rayleigh scattering, in which shorter (blue) wavelengths of light are scattered more than longer (red) wavelengths by the tiny molecules of gases in the atmosphere.\n\nIn the evening, as the sun sets, the sky can take on a range of colors, including orange, pink, and purple, due to the scattering of light by atmospheric particles. During sunrise and sunset, the sky can also appear more red or orange due to the longer wavelengths of light being scattered.\n\nAt night, the sky can appear dark, but it can also be illuminated by the moon, stars, and artificial light sources such as city lights. In areas with minimal light pollution, the night sky can be a deep indigo or black, with the stars and constellations visible as points of light.\n\nOverall, the color of the sky can vary greatly depending on the time of day, atmospheric conditions, and the observer's location.",
  "prompt_token_count": 9,
  "generation_token_count": 256,
  "stop_reason": "stop"
}
```

### Embedding Models

Not supported by Llama 2.
