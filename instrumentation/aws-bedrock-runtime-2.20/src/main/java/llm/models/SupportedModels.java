/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

/**
 * Prefixes for supported models. As long as the model ID for an invoked LLM model contains
 * one of these prefixes the instrumentation should attempt to process the request/response.
 * <p>
 * See the README for each model in llm.models.* for more details on supported models.
 */
public class SupportedModels {
    public static final String ANTHROPIC_CLAUDE = "anthropic.claude";
    public static final String AMAZON_TITAN = "amazon.titan";
    public static final String META_LLAMA_2 = "meta.llama2";
    public static final String COHERE_COMMAND = "cohere.command";
    public static final String COHERE_EMBED = "cohere.embed";
    public static final String AI_21_LABS_JURASSIC = "ai21.j2";
}
