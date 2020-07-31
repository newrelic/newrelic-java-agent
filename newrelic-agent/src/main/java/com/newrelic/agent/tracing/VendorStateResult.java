/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class VendorStateResult {
    private final List<String> vendorStates;
    private final String nrState;

    VendorStateResult(List<String> vendorStates, String nrState) {
        this.vendorStates = Collections.unmodifiableList(new ArrayList<>(vendorStates));
        this.nrState = nrState;
    }

    List<String> getVendorStates() {
        return vendorStates;
    }

    String getNrState() {
        return nrState;
    }
}
