/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.iface;

public interface SampleInterfaceObject extends SampleSuperInterfaceObject {

    long getTestLong();

    int getTestInt();

    int getTestIntWahoo();

    long getTestLongWahoo();

    int getTestIntYipee();

}
