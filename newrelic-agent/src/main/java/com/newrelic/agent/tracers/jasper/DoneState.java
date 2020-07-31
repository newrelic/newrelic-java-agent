/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;

/**
 * A do-nothing state for after footer is inserted.
 */
public class DoneState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {
        writeText(tx, generator, node, text);
        return this;
    }
}
