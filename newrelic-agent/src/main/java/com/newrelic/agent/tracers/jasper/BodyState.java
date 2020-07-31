/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.util.regex.Matcher;

import com.newrelic.agent.Transaction;

/**
 * A state for adding footer after header has been inserted.
 */
public class BodyState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {

        Matcher matcher = SCRIPT_PATTERN.matcher(text);
        if (matcher.find()) {
            String begin = text.substring(0, matcher.start());
            RUMState state = this.process(tx, generator, node, begin);
            String s = text.substring(matcher.start());
            if (state == DONE_STATE) {
                return DONE_STATE.process(tx, generator, node, s);
            } else {
                return SCRIPT_STATE.process(tx, generator, node, s);
            }
        }

        matcher = BODY_END_PATTERN.matcher(text);
        if (matcher.find()) {
            String s = text.substring(0, matcher.start());
            writeText(tx, generator, node, s);
            writeFooter(generator);
            s = text.substring(matcher.start());
            return DONE_STATE.process(tx, generator, node, s);
        }
        matcher = HTML_END_PATTERN.matcher(text);
        if (matcher.find()) {
            String s = text.substring(0, matcher.start());
            writeText(tx, generator, node, s);
            writeFooter(generator);
            s = text.substring(matcher.start());
            return DONE_STATE.process(tx, generator, node, s);
        }

        writeText(tx, generator, node, text);
        return this;
    }
}
