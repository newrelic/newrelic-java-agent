/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

public interface MyOriginalInterface {

    public boolean isInterfaceWeaved();

    public String getMemberField();

    public String getStaticField();
}
