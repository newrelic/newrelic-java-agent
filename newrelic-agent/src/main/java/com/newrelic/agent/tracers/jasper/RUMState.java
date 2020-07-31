/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;

public interface RUMState {

    RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception;
}
