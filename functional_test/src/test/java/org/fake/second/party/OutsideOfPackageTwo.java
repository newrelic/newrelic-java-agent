/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.fake.second.party;

import com.fake.second.party.ClassOne;

public class OutsideOfPackageTwo {

    private final ClassOne classOne;

    public OutsideOfPackageTwo(ClassOne classOne) {
        this.classOne = classOne;
    }

    public void outsidePackageTwoWork() throws Exception {
        Thread.sleep(25);
        classOne.classOneWork();
    }
}
