/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.ktor.util.pipeline

import com.newrelic.api.agent.weaver.SkipIfPresent

@SkipIfPresent(originalName = "io.ktor.util.pipeline.PipelineExecutor")
class PipelineExecutor_SkipIfPresent<R> {

}