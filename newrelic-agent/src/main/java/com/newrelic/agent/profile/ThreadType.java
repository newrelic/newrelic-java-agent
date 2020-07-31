/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;

import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

public interface ThreadType extends JSONStreamAware, JSONAware {

    String getName();

    public enum BasicThreadType implements ThreadType {
        AGENT("agent"), // agent worker, harvest threads
        AGENT_INSTRUMENTATION("agent_instrumentation"), // threads that are executing instrumentation code
        REQUEST("request"), BACKGROUND("background"), OTHER("other");

        private String name;

        private BasicThreadType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void writeJSONString(Writer out) throws IOException {
            JSONValue.writeJSONString(name, out);
        }

        @Override
        public String toJSONString() {
            return name;
        }
    }
}
