/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.second.party;

public class ClassFour {
    private final ClassFive classFive;

    public ClassFour(ClassFive classFive) {
        this.classFive = classFive;
    }

    public void classFourWork() throws Exception {
        System.out.println("Class four work");

        classFive.classFiveWork();
    }

}
