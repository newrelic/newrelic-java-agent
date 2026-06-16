/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.kafka.clients.consumer.internals;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * This class was removed on Kafka 3. So this will prevent the module from applying on older versions.
 */
@SkipIfPresent
public interface PartitionAssignor {
}
