/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
 * Designed to reduce memory. This fixed size json array is not designed 
 * to actually limit the numbers in the list. If you add more than originally allocated then it will
 * increase the size.
 */
public class InitialSizedJsonArray implements JSONStreamAware {

    private List<Object> toSend;

    public InitialSizedJsonArray(int size) {
        if (size > 0) {
            toSend = new ArrayList<>(size);
        } else {
            toSend = Collections.emptyList();
        }
    }

    public void add(Object obj) {
        toSend.add(obj);
    }

    public void addAll(Collection<Object> objs) {
        toSend.addAll(objs);
    }

    public int size() {
        return toSend.size();
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(toSend, out);
    }

}
