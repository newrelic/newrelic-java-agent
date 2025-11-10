/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.rabbitmq.client;

import com.newrelic.api.agent.weaver.Weave;

/**
 * This class is weaved only to foce this module to fail in rabbitmq 2.5.0 and above.
 */
@Weave(originalName = "com.rabbitmq.client.FileProperties")
public class FileProperties_Instrumentation {
}
