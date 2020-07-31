/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import static com.newrelic.agent.tracers.jasper.AbstractRUMState.END_SCRIPT_PATTERN;

import java.util.regex.Matcher;

import com.newrelic.agent.Transaction;

/**
 * A state for ignoring script tags.
 */
public class ScriptPostHeaderState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {
        Matcher matcher = END_SCRIPT_PATTERN.matcher(text);
        if (matcher.find()) {
            String s = text.substring(0, matcher.end());
            writeText(tx, generator, node, s);
            s = text.substring(matcher.end());
            return BODY_STATE.process(tx, generator, node, s);
        }

        writeText(tx, generator, node, text);
        return this;
    }

}
