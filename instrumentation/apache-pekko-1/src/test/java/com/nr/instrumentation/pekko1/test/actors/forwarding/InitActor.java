/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.pekko1.test.actors.forwarding;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.Actor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.japi.Creator;
import org.apache.pekko.japi.pf.FI;
import org.apache.pekko.japi.pf.ReceiveBuilder;

public class InitActor extends AbstractActor {
    private static Props props = Props.create(new Creator<Actor>() {
        @Override
        public Actor create() {
            return new ForwardActor();
        }
    });

    private ActorRef forwardActor = getContext().actorOf(props, "forwardActor");

    @Override
    public Receive createReceive() {
        return new ReceiveBuilder().matchAny(new FI.UnitApply<Object>() {
            @Override
            public void apply(Object message) throws Exception {
                forwardActor.forward(message, getContext());
            }
        }).build();
    }
}