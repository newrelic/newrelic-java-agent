/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

public class UninstrumentMeObject implements UninstrumentInterfaceObject {

    private String lastName;

    String getName(String prefix) {
        return prefix + "dude";
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

    public String getMiddleName() {
        return "dragon";
    }

}
