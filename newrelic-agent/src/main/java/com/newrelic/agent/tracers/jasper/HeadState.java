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
 * A state for finding the head tag. If we see a script tag before the head tag, then we are just going to abort
 * instrumentation.
 */
public class HeadState extends AbstractRUMState {

    @Override
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text) throws Exception {

        Matcher scriptMatcher = SCRIPT_PATTERN.matcher(text);
        Integer scriptIndex = null;
        if (scriptMatcher.find()) {
            scriptIndex = scriptMatcher.end();
        }

        Matcher matcher = HEAD_PATTERN.matcher(text);
        if (matcher.find()) {
            if (isScriptFirst(scriptIndex, matcher.end())) {
                return PRE_HEAD_SCRIPT_STATE.process(tx, generator, node, text);
            }
            String s = text.substring(0, matcher.end());
            writeText(tx, generator, node, s);
            s = text.substring(matcher.end());
            return PRE_META_STATE.process(tx, generator, node, s);

        }

        matcher = HEAD_END_PATTERN.matcher(text);
        if (matcher.find()) {
            if (isScriptFirst(scriptIndex, matcher.end())) {
                // write the head before the script
                String s = text.substring(0, scriptMatcher.start());
                writeText(tx, generator, node, s);
                writeHeader(generator);
                s = text.substring(scriptMatcher.start());
                return SCRIPT_STATE.process(tx, generator, node, s);
            }
            String s = text.substring(0, matcher.start());
            writeText(tx, generator, node, s);
            writeHeader(generator);
            s = text.substring(matcher.start());
            return BODY_STATE.process(tx, generator, node, s);
        }
        matcher = BODY_END_PATTERN.matcher(text);
        if (matcher.find()) {
            if (isScriptFirst(scriptIndex, matcher.end())) {
                // body state also checks for script tags - but no need to do the extra parsing
                return SCRIPT_STATE.process(tx, generator, node, text);
            }
            return BODY_STATE.process(tx, generator, node, text);
        }

        matcher = BODY_START_PATTERN.matcher(text);
        if (scriptIndex != null) {
            if (matcher.find()) {
                if (isScriptFirst(scriptIndex, matcher.end())) {
                    return PRE_HEAD_SCRIPT_STATE.process(tx, generator, node, text);
                } else {
                    return SCRIPT_STATE.process(tx, generator, node, text);
                }
            } else {
                // else go into pre head script state - we might just have not seen the head yet
                return PRE_HEAD_SCRIPT_STATE.process(tx, generator, node, text);
            }
        }

        writeText(tx, generator, node, text);
        return this;
    }

    private boolean isScriptFirst(Integer scriptIndex, Integer headIndex) {
        if (scriptIndex == null) {
            return false;
        } else if (headIndex == null) {
            return true;
        } else {
            return headIndex > scriptIndex;
        }
    }

}
