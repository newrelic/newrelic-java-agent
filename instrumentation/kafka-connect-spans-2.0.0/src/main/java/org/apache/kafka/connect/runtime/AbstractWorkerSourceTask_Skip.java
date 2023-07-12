/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.connect.runtime;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * This class was introduced in Kafka 3 and is a base for 2 WorkerSourceTasks.
 */
@SkipIfPresent(originalName = "org.apache.kafka.connect.runtime.AbstractWorkerSourceTask")
public abstract class AbstractWorkerSourceTask_Skip {
}
