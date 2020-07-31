/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.akka22.test.actors.forwarding;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;

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