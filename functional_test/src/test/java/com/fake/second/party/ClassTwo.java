/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.second.party;

public class ClassTwo {

    private final ClassThree classThree;

    public ClassTwo(ClassThree classThree) {
        this.classThree = classThree;
    }

    public void classTwoWork() throws Exception {
        System.out.println("Class two work");

        Thread.sleep(50);
        classThree.classThreeWork();
    }

}
