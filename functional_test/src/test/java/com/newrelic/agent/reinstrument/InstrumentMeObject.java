/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.api.agent.Trace;

public class InstrumentMeObject implements InstrumentInterfaceObject {
    private String lastName;

    String getName(String prefix) {
        return prefix + "dude";
    }

    public String getAnotherMethodWahoo() {
        return "foo";
    }

    public int getAge() {
        return 19;
    }

    public int getHeight() {
        return 56;
    }

    public void setLastName(String pName) {
        lastName = pName;
    }

    public int getShoeSize() {
        return 8;
    }

    public String getHairColor() {
        return "red";
    }

    public int getAgeInXYears(int years) {
        return getAge() + years;
    }

    public InstrumentMeReturnType method1() {
        return new InstrumentMeReturnType(5L);
    }

    public InstrumentMeReturnType method2(int count) {
        return new InstrumentMeReturnType(7L + count);
    }

    public InstrumentMeReturnType method3(String tada) {
        return new InstrumentMeReturnType(8L);
    }

    @Trace(dispatcher = true)
    public long bMethod1234(String val) {
        return 6L;
    }

    @InstrumentAnnotation
    public long aMethod1() {
        return 5L;
    }

    @InstrumentAnnotation
    public long aMethod2(int count) {
        return 7L + count;
    }

    @InstrumentAnnotation
    public long aMethod3(String tada) {
        return 8L;
    }

}
