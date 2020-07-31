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
 * A state for inserting header after finding head tag.
 */
public class MetaState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {

        Matcher tagMatcher = END_TAG_OR_QUOTE_PATTERN.matcher(text);
        if (tagMatcher.find()) {

            if (tagMatcher.group().equals("\"")) {
                // we are inside a quote - speed to end
                String s = text.substring(0, tagMatcher.end());
                writeText(tx, generator, node, s);
                s = text.substring(tagMatcher.end());
                return QUOTE_STATE.process(tx, generator, node, s);
            } else if (tagMatcher.group().equals("'")) {
                // we are inside a quote - speed to end
                String s = text.substring(0, tagMatcher.end());
                writeText(tx, generator, node, s);
                s = text.substring(tagMatcher.end());
                return SINGLE_QUOTE_STATE.process(tx, generator, node, s);
            } else {
                // we have reached the end of the meta
                String s = text.substring(0, tagMatcher.start());
                writeText(tx, generator, node, s);
                s = text.substring(tagMatcher.start());
                return PRE_META_STATE.process(tx, generator, node, s);
            }
        }

        writeText(tx, generator, node, text);
        return this;
    }
}
