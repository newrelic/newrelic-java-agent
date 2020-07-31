/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

public class BearConcrete extends BearBaseClass implements BearInterface {

    @Override
    public boolean isAlsoWeaved() {
        return false;
    }
}