/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.normalization;

import java.util.Set;

public class TransactionSegmentTerms {
    final String prefix;
    final Set<String> terms;

    public TransactionSegmentTerms(String prefix, Set<String> terms) {
        super();
        this.prefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        this.terms = terms;
    }

    @Override
    public String toString() {
        return "TransactionSegmentTerms [prefix=" + prefix + ", terms=" + terms + "]";
    }

}
