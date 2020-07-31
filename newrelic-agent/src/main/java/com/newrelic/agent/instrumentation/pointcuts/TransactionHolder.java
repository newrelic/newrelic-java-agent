/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts;


// This class must load in the bootstrap classloader because it's a dependency of an InterfaceMixin.
// JAVA-609. See BootstrapLoader.addMixinInterfacesToBootstrap().
@LoadOnBootstrap
public interface TransactionHolder {

    Object _nr_getTransaction();

    void _nr_setTransaction(Object tx);

    Object _nr_getName();

    void _nr_setName(Object tx);

}
