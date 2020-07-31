/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.rabbitmq.client.impl.recovery;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DeliverCallback;

/**
 * This class is here to prevent versions < 5.0.0 from matching
 */
@Weave(type = MatchType.ExactClass, originalName = "com.rabbitmq.client.impl.recovery.AutorecoveringChannel")
public class AutorecoveringChannel_Instrumentation {

    private Consumer consumerFromDeliverCancelCallbacks(final DeliverCallback deliverCallback,
                                                        final CancelCallback cancelCallback) {
        return Weaver.callOriginal();
    }

}
