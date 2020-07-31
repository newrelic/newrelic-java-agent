/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.second.party;

import org.fake.second.party.OutsideOfPackageOne;
import org.fake.second.party.OutsideOfPackageTwo;

public class GraphTest {

    static final ClassThree classThree = new ClassThree();
    static final ClassTwo classTwo = new ClassTwo(classThree);
    static final ClassOne classOne = new ClassOne(classTwo, classThree);

    static final OutsideOfPackageTwo outsideOfPackageTwo = new OutsideOfPackageTwo(classOne);
    static final OutsideOfPackageOne outsideOfPackageOne = new OutsideOfPackageOne(outsideOfPackageTwo);

    public static void main(String[] args) {
        // First, test direct callers in the same package
        reallyRun();
        sleep(5);

        // Second, test callers that require a root node to be transformed first
        rootRunTest();
        sleep(5);
    }

    private static void reallyRun() {
        try {
            classOne.classOneWork();
        } catch (Exception e) {
        }
    }

    private static void rootRunTest() {
        try {
            // This should be outside of the graph that we pick up automatically
            outsideOfPackageOne.outsidePackageOneWork();
        } catch (Exception e) {
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }

    private static void notCalled() {
    }

}
