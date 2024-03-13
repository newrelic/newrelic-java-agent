/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekko1.test.actors.broadcasting.branches;

import org.apache.pekko.actor.UntypedAbstractActor;

public class ChildActor extends UntypedAbstractActor {
    private static int count = 0;

    @Override
    public void onReceive(Object message) throws Exception {
        if (count == 1000) {
            return;
        }

        count++;
        self().forward(message, getContext());
    }
}
