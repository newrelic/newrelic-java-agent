/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.log4j;

// Mockito was not playing with JUnit4 tests so since the Category constructor is protected
// Created this test factory in the same package to avoid needing an actual mock
public class TestCategoryFactory {
    public static Category create(String loggerName) {
        return new Category(loggerName);
    }
}
