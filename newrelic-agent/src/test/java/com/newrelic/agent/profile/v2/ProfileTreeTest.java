/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

public class ProfileTreeTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testJson() throws IOException {
        IProfile profile = Mockito.mock(IProfile.class);
        ProfileTree tree = new ProfileTree(profile, true);

        String json = toJsonString(tree);

        List<Object> list = (List<Object>) JSONValue.parse(json);
        Assert.assertEquals(1, list.size());
        Map<?, ?> map = (Map<?, ?>) list.get(0);
        Assert.assertEquals(0L, map.get("cpu_time"));
    }

    public static String toJsonString(JSONStreamAware obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        obj.writeJSONString(writer);

        writer.flush();
        return out.toString();
    }
}
