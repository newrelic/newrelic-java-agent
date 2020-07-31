/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.second.party;

public class ClassOne {

    private final ClassTwo classTwo;
    private final ClassThree classThree;

    public ClassOne(ClassTwo classTwo, ClassThree classThree) {
        this.classTwo = classTwo;
        this.classThree = classThree;
    }

    public void classOneWork() throws Exception {
        System.out.println("Class one work");

        classTwo.classTwoWork();
        classThree.classThreeWork();
    }

}
