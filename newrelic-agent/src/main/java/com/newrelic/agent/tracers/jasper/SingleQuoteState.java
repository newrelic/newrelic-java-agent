/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.util.regex.Matcher;

import com.newrelic.agent.Transaction;

public class SingleQuoteState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {

        Matcher nextQuoteMatcher = SINGLE_QUOTE_PATTERN.matcher(text);

        if (nextQuoteMatcher.find()) {
            String s = text.substring(0, nextQuoteMatcher.end());
            writeText(tx, generator, node, s);
            s = text.substring(nextQuoteMatcher.end());
            return META_STATE.process(tx, generator, node, s);
        }

        // this is here just in case - we really should never get here
        Matcher matcher = HEAD_END_PATTERN.matcher(text);
        if (matcher.find()) {
            String s = text.substring(0, matcher.start());
            writeText(tx, generator, node, s);
            writeHeader(generator);
            s = text.substring(matcher.start());
            return BODY_STATE.process(tx, generator, node, s);
        }

        writeText(tx, generator, node, text);
        return this;

    }

}
