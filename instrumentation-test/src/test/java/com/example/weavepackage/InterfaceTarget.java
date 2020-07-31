/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.example.weavepackage;

public class InterfaceTarget implements InterfaceOriginal {
    @Override
    public String interfaceMethod() {
        return "interface method";
    }
}
