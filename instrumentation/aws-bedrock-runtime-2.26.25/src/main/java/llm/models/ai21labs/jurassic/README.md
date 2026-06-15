# AI21 Labs

Examples of the request/response bodies for models that have been tested and verified to work. The instrumentation should continue to correctly process new
models as long as they match the model naming prefixes in `llm.models.SupportedModels` and the request/response structure stays the same as the examples listed
here.

## Jurassic Models

#### Text Completion Models

The following models have been tested:

* Jurassic-2 Mid (`ai21.j2-mid-v1`)
* Jurassic-2 Ultra (`ai21.j2-ultra-v1`)

#### Sample Request

```json
{
  "temperature": 0.5,
  "maxTokens": 1000,
  "prompt": "What is the color of the sky?"
}
```

#### Sample Response

```json
{
  "id": 1234,
  "prompt": {
    "text": "What is the color of the sky?",
    "tokens": [
      {
        "generatedToken": {
          "token": "▁What▁is▁the",
          "logprob": -8.316551208496094,
          "raw_logprob": -8.316551208496094
        },
        "topTokens": null,
        "textRange": {
          "start": 0,
          "end": 11
        }
      },
      {
        "generatedToken": {
          "token": "▁color",
          "logprob": -7.189708709716797,
          "raw_logprob": -7.189708709716797
        },
        "topTokens": null,
        "textRange": {
          "start": 11,
          "end": 17
        }
      },
      {
        "generatedToken": {
          "token": "▁of▁the▁sky",
          "logprob": -5.750617027282715,
          "raw_logprob": -5.750617027282715
        },
        "topTokens": null,
        "textRange": {
          "start": 17,
          "end": 28
        }
      },
      {
        "generatedToken": {
          "token": "?",
          "logprob": -5.858178615570068,
          "raw_logprob": -5.858178615570068
        },
        "topTokens": null,
        "textRange": {
          "start": 28,
          "end": 29
        }
      }
    ]
  },
  "completions": [
    {
      "data": {
        "text": "\nThe color of the sky on Earth is blue. This is because Earth's atmosphere scatters short-wavelength light more efficiently than long-wavelength light. When sunlight enters Earth's atmosphere, most of the blue light is scattered, leaving mostly red light to illuminate the sky. The scattering of blue light is more efficient because it travels as shorter, smaller waves.",
        "tokens": [
          {
            "generatedToken": {
              "token": "<|newline|>",
              "logprob": 0.0,
              "raw_logprob": -6.305972783593461E-5
            },
            "topTokens": null,
            "textRange": {
              "start": 0,
              "end": 1
            }
          },
          {
            "generatedToken": {
              "token": "▁The▁color",
              "logprob": -0.007753042038530111,
              "raw_logprob": -0.18397575616836548
            },
            "topTokens": null,
            "textRange": {
              "start": 1,
              "end": 10
            }
          },
          {
            "generatedToken": {
              "token": "▁of▁the▁sky",
              "logprob": -6.770858453819528E-5,
              "raw_logprob": -0.0130088459700346
            },
            "topTokens": null,
            "textRange": {
              "start": 10,
              "end": 21
            }
          },
          {
            "generatedToken": {
              "token": "▁on▁Earth",
              "logprob": -6.189814303070307E-4,
              "raw_logprob": -0.06852064281702042
            },
            "topTokens": null,
            "textRange": {
              "start": 21,
              "end": 30
            }
          },
          {
            "generatedToken": {
              "token": "▁is",
              "logprob": -0.5599813461303711,
              "raw_logprob": -1.532042145729065
            },
            "topTokens": null,
            "textRange": {
              "start": 30,
              "end": 33
            }
          },
          {
            "generatedToken": {
              "token": "▁blue",
              "logprob": -0.0358763113617897,
              "raw_logprob": -0.2531339228153229
            },
            "topTokens": null,
            "textRange": {
              "start": 33,
              "end": 38
            }
          },
          {
            "generatedToken": {
              "token": ".",
              "logprob": -0.0022088908590376377,
              "raw_logprob": -0.11807831376791
            },
            "topTokens": null,
            "textRange": {
              "start": 38,
              "end": 39
            }
          },
          {
            "generatedToken": {
              "token": "▁This▁is▁because",
              "logprob": -0.7582850456237793,
              "raw_logprob": -1.6503678560256958
            },
            "topTokens": null,
            "textRange": {
              "start": 39,
              "end": 55
            }
          },
          {
            "generatedToken": {
              "token": "▁Earth's▁atmosphere",
              "logprob": -0.37150290608406067,
              "raw_logprob": -1.086639404296875
            },
            "topTokens": null,
            "textRange": {
              "start": 55,
              "end": 74
            }
          },
          {
            "generatedToken": {
              "token": "▁scatter",
              "logprob": -1.4662635294371285E-5,
              "raw_logprob": -0.011443688534200191
            },
            "topTokens": null,
            "textRange": {
              "start": 74,
              "end": 82
            }
          },
          {
            "generatedToken": {
              "token": "s",
              "logprob": -9.929640509653836E-5,
              "raw_logprob": -0.01099079567939043
            },
            "topTokens": null,
            "textRange": {
              "start": 82,
              "end": 83
            }
          },
          {
            "generatedToken": {
              "token": "▁short",
              "logprob": -2.97943115234375,
              "raw_logprob": -1.8346563577651978
            },
            "topTokens": null,
            "textRange": {
              "start": 83,
              "end": 89
            }
          },
          {
            "generatedToken": {
              "token": "-wavelength",
              "logprob": -1.5722469834145159E-4,
              "raw_logprob": -0.020076051354408264
            },
            "topTokens": null,
            "textRange": {
              "start": 89,
              "end": 100
            }
          },
          {
            "generatedToken": {
              "token": "▁light",
              "logprob": -1.8000440832111053E-5,
              "raw_logprob": -0.008328350260853767
            },
            "topTokens": null,
            "textRange": {
              "start": 100,
              "end": 106
            }
          },
          {
            "generatedToken": {
              "token": "▁more▁efficiently",
              "logprob": -0.11763446033000946,
              "raw_logprob": -0.6382070779800415
            },
            "topTokens": null,
            "textRange": {
              "start": 106,
              "end": 123
            }
          },
          {
            "generatedToken": {
              "token": "▁than",
              "logprob": -0.0850396677851677,
              "raw_logprob": -0.4660969078540802
            },
            "topTokens": null,
            "textRange": {
              "start": 123,
              "end": 128
            }
          },
          {
            "generatedToken": {
              "token": "▁long",
              "logprob": -0.21488533914089203,
              "raw_logprob": -0.43275904655456543
            },
            "topTokens": null,
            "textRange": {
              "start": 128,
              "end": 133
            }
          },
          {
            "generatedToken": {
              "token": "-wavelength",
              "logprob": -3.576272320060525E-6,
              "raw_logprob": -0.0032024311367422342
            },
            "topTokens": null,
            "textRange": {
              "start": 133,
              "end": 144
            }
          },
          {
            "generatedToken": {
              "token": "▁light",
              "logprob": -6.603976362384856E-5,
              "raw_logprob": -0.021542951464653015
            },
            "topTokens": null,
            "textRange": {
              "start": 144,
              "end": 150
            }
          },
          {
            "generatedToken": {
              "token": ".",
              "logprob": -0.03969373181462288,
              "raw_logprob": -0.24834078550338745
            },
            "topTokens": null,
            "textRange": {
              "start": 150,
              "end": 151
            }
          },
          {
            "generatedToken": {
              "token": "▁When",
              "logprob": -0.8459960222244263,
              "raw_logprob": -1.758193016052246
            },
            "topTokens": null,
            "textRange": {
              "start": 151,
              "end": 156
            }
          },
          {
            "generatedToken": {
              "token": "▁sunlight",
              "logprob": -0.043000709265470505,
              "raw_logprob": -0.413555383682251
            },
            "topTokens": null,
            "textRange": {
              "start": 156,
              "end": 165
            }
          },
          {
            "generatedToken": {
              "token": "▁enters",
              "logprob": -2.2813825607299805,
              "raw_logprob": -1.975184440612793
            },
            "topTokens": null,
            "textRange": {
              "start": 165,
              "end": 172
            }
          },
          {
            "generatedToken": {
              "token": "▁Earth's▁atmosphere",
              "logprob": -0.04206264019012451,
              "raw_logprob": -0.22090668976306915
            },
            "topTokens": null,
            "textRange": {
              "start": 172,
              "end": 191
            }
          },
          {
            "generatedToken": {
              "token": ",",
              "logprob": -2.1300431399140507E-4,
              "raw_logprob": -0.04065611585974693
            },
            "topTokens": null,
            "textRange": {
              "start": 191,
              "end": 192
            }
          },
          {
            "generatedToken": {
              "token": "▁most▁of▁the",
              "logprob": -1.0895559787750244,
              "raw_logprob": -1.4258980751037598
            },
            "topTokens": null,
            "textRange": {
              "start": 192,
              "end": 204
            }
          },
          {
            "generatedToken": {
              "token": "▁blue▁light",
              "logprob": -2.7195115089416504,
              "raw_logprob": -2.069707155227661
            },
            "topTokens": null,
            "textRange": {
              "start": 204,
              "end": 215
            }
          },
          {
            "generatedToken": {
              "token": "▁is",
              "logprob": -3.036991402041167E-4,
              "raw_logprob": -0.036258988082408905
            },
            "topTokens": null,
            "textRange": {
              "start": 215,
              "end": 218
            }
          },
          {
            "generatedToken": {
              "token": "▁scattered",
              "logprob": -1.1086402082582936E-5,
              "raw_logprob": -0.007142604328691959
            },
            "topTokens": null,
            "textRange": {
              "start": 218,
              "end": 228
            }
          },
          {
            "generatedToken": {
              "token": ",",
              "logprob": -0.8132423162460327,
              "raw_logprob": -1.204469919204712
            },
            "topTokens": null,
            "textRange": {
              "start": 228,
              "end": 229
            }
          },
          {
            "generatedToken": {
              "token": "▁leaving",
              "logprob": -0.028648898005485535,
              "raw_logprob": -0.24427929520606995
            },
            "topTokens": null,
            "textRange": {
              "start": 229,
              "end": 237
            }
          },
          {
            "generatedToken": {
              "token": "▁mostly",
              "logprob": -0.012762418016791344,
              "raw_logprob": -0.18833962082862854
            },
            "topTokens": null,
            "textRange": {
              "start": 237,
              "end": 244
            }
          },
          {
            "generatedToken": {
              "token": "▁red▁light",
              "logprob": -0.3875422477722168,
              "raw_logprob": -0.9608176350593567
            },
            "topTokens": null,
            "textRange": {
              "start": 244,
              "end": 254
            }
          },
          {
            "generatedToken": {
              "token": "▁to▁illuminate",
              "logprob": -1.2177848815917969,
              "raw_logprob": -1.6379175186157227
            },
            "topTokens": null,
            "textRange": {
              "start": 254,
              "end": 268
            }
          },
          {
            "generatedToken": {
              "token": "▁the▁sky",
              "logprob": -0.004821578972041607,
              "raw_logprob": -0.1349806934595108
            },
            "topTokens": null,
            "textRange": {
              "start": 268,
              "end": 276
            }
          },
          {
            "generatedToken": {
              "token": ".",
              "logprob": -2.7894584491150454E-5,
              "raw_logprob": -0.01649152860045433
            },
            "topTokens": null,
            "textRange": {
              "start": 276,
              "end": 277
            }
          },
          {
            "generatedToken": {
              "token": "▁The",
              "logprob": -4.816740989685059,
              "raw_logprob": -3.04256534576416
            },
            "topTokens": null,
            "textRange": {
              "start": 277,
              "end": 281
            }
          },
          {
            "generatedToken": {
              "token": "▁scattering",
              "logprob": -0.07598043233156204,
              "raw_logprob": -0.4935254752635956
            },
            "topTokens": null,
            "textRange": {
              "start": 281,
              "end": 292
            }
          },
          {
            "generatedToken": {
              "token": "▁of",
              "logprob": -2.1653952598571777,
              "raw_logprob": -2.153515338897705
            },
            "topTokens": null,
            "textRange": {
              "start": 292,
              "end": 295
            }
          },
          {
            "generatedToken": {
              "token": "▁blue▁light",
              "logprob": -0.0025517542380839586,
              "raw_logprob": -0.0987434908747673
            },
            "topTokens": null,
            "textRange": {
              "start": 295,
              "end": 306
            }
          },
          {
            "generatedToken": {
              "token": "▁is",
              "logprob": -0.04848421365022659,
              "raw_logprob": -0.5477231740951538
            },
            "topTokens": null,
            "textRange": {
              "start": 306,
              "end": 309
            }
          },
          {
            "generatedToken": {
              "token": "▁more▁efficient",
              "logprob": -1.145136833190918,
              "raw_logprob": -1.6279737949371338
            },
            "topTokens": null,
            "textRange": {
              "start": 309,
              "end": 324
            }
          },
          {
            "generatedToken": {
              "token": "▁because▁it",
              "logprob": -0.7712448835372925,
              "raw_logprob": -1.402230143547058
            },
            "topTokens": null,
            "textRange": {
              "start": 324,
              "end": 335
            }
          },
          {
            "generatedToken": {
              "token": "▁travels",
              "logprob": -1.0001159535022452E-4,
              "raw_logprob": -0.03441037982702255
            },
            "topTokens": null,
            "textRange": {
              "start": 335,
              "end": 343
            }
          },
          {
            "generatedToken": {
              "token": "▁as",
              "logprob": -2.169585604860913E-5,
              "raw_logprob": -0.008925186470150948
            },
            "topTokens": null,
            "textRange": {
              "start": 343,
              "end": 346
            }
          },
          {
            "generatedToken": {
              "token": "▁shorter",
              "logprob": -0.0026372435968369246,
              "raw_logprob": -0.054399896413087845
            },
            "topTokens": null,
            "textRange": {
              "start": 346,
              "end": 354
            }
          },
          {
            "generatedToken": {
              "token": ",",
              "logprob": -3.576214658096433E-5,
              "raw_logprob": -0.011654269881546497
            },
            "topTokens": null,
            "textRange": {
              "start": 354,
              "end": 355
            }
          },
          {
            "generatedToken": {
              "token": "▁smaller",
              "logprob": -1.0609570381348021E-5,
              "raw_logprob": -0.007282733917236328
            },
            "topTokens": null,
            "textRange": {
              "start": 355,
              "end": 363
            }
          },
          {
            "generatedToken": {
              "token": "▁waves",
              "logprob": -2.7418097943154862E-6,
              "raw_logprob": -0.0030873988289386034
            },
            "topTokens": null,
            "textRange": {
              "start": 363,
              "end": 369
            }
          },
          {
            "generatedToken": {
              "token": ".",
              "logprob": -0.19333261251449585,
              "raw_logprob": -0.535153865814209
            },
            "topTokens": null,
            "textRange": {
              "start": 369,
              "end": 370
            }
          },
          {
            "generatedToken": {
              "token": "<|endoftext|>",
              "logprob": -0.03163028880953789,
              "raw_logprob": -0.6691970229148865
            },
            "topTokens": null,
            "textRange": {
              "start": 370,
              "end": 370
            }
          }
        ]
      },
      "finishReason": {
        "reason": "endoftext"
      }
    }
  ]
}
```

### Embedding Models

Not supported by Jurassic.