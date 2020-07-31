/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.second.party;

public class ClassThree {

    public void classThreeWork() throws Exception {
        System.out.println("Class three work");

        ClassFour classFour = new ClassFour(new ClassFive());
        classFour.classFourWork();

        Thread.sleep(10);
    }

}
