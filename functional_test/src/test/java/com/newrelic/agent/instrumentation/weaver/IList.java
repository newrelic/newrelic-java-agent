/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

public interface IList<E> {
    int count();

    boolean remove(E item);

    Object instanceFieldTest();

    Object staticFieldTest();

    int unimplementedWeaveMethodTest();

    int unimplementedWeaveMethod();
}
