/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jakarta.ws.rs.api;

// The order of the interfaces on this class is important in order to reproduce a bug. ** DO NOT MODIFY **
public class UserServiceImpl implements UserService, UserFeaturesResource {

    private UserFeaturesResource userFeaturesResource = new DefaultUserFeaturesImpl();

    @Override
    public String getUserFeatures() {
        return userFeaturesResource.getUserFeatures();
    }

}
