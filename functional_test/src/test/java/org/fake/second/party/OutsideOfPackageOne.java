/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.fake.second.party;

public class OutsideOfPackageOne {

    private final OutsideOfPackageTwo outsideOfPackageTwo;

    public OutsideOfPackageOne(OutsideOfPackageTwo outsideOfPackageTwo) {
        this.outsideOfPackageTwo = outsideOfPackageTwo;
    }

    public void outsidePackageOneWork() throws Exception {
        outsideOfPackageTwo.outsidePackageTwoWork();
    }

}
