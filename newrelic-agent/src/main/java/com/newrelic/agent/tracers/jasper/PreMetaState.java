/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers.jasper;

import java.util.regex.Matcher;

import com.newrelic.agent.Transaction;

public class PreMetaState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {

        Matcher tagMatcher = START_TAG_PATTERN.matcher(text);

        if (tagMatcher.find()) {
            String begin = text.substring(0, tagMatcher.start());
            writeText(tx, generator, node, begin);
            String s = text.substring(tagMatcher.start());

            if (s.startsWith("</head>")) {
                // we have reached the end of the header
                writeHeader(generator);
                return BODY_STATE.process(tx, generator, node, s);
            } else if (s.startsWith("<meta ") || s.startsWith("<META")) {
                // we are inside a meta state
                return META_STATE.process(tx, generator, node, s);
            } else if (s.startsWith("<title>")) {
                return TITLE_STATE.process(tx, generator, node, s);
            } else if (s.startsWith("<!--")) {
                return COMMENT_STATE.process(tx, generator, node, s);
            } else {
                // we have reached some other node
                writeHeader(generator);
                return BODY_STATE.process(tx, generator, node, s);
            }
        }

        writeText(tx, generator, node, text);
        return this;
    }

}
