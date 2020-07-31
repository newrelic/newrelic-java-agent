/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.broadcasting.branches;

import akka.actor.UntypedActor;

public class ChildActor extends UntypedActor {
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
