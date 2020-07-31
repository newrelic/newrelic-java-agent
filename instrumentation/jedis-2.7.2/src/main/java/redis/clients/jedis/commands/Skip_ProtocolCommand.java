/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package redis.clients.jedis.commands;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// only exists in jedis 3.0+
@SkipIfPresent(originalName = "redis.clients.jedis.commands.ProtocolCommand")
public class Skip_ProtocolCommand {
}
