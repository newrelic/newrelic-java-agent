/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.javax.ws.rs.api;

public class InterfaceResourceImpl implements InterfaceResource {
    @Override
    public String getIt() {
        return "Got it from the interface!";
    }

    @Override
    public String exceptionTest() {
        String data;
        try {
            data = getData();
        } catch (RuntimeException e) {
            return "Oops!";
        }

        return data;
    }

    private String getData() throws RuntimeException {
        return "Got it from the interface!";
    }

}
