/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake.third.party.instrumentation.weaver.testclasses;

public class CaptainPlanet extends ASuperhero implements Earth, Water, Wind, Fire {

    @Override
    public String getRing() {
        return "Captain Planet Ring";
    }

    @Override
    public String fire() {
        return "FIRE";
    }

    @Override
    public String wind() {
        return "WIND";
    }

    @Override
    public String water() {
        return "WATER";
    }

    @Override
    public String earth() {
        return "EARTH";
    }

    @Override
    public String getName() {
        return "Captain Planet";
    }
}
