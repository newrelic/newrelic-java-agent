/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

public class OriginalNameInterfaceImpl implements OriginalNameInterface {

    @Override
    public void foo() {
        System.out.println("This is foo");
    }

    @Override
    public void bar() {
        System.out.println("This is bar");
    }

}
