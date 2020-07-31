/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.newrelic.agent.util.StringMap;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.Strings;

public class ProfiledMethodFactoryTest {

    @Test
    public void tracers() throws IOException {
        IProfile profile = Mockito.mock(IProfile.class);
        Mockito.when(profile.getStringMap()).thenReturn(StringMap.NO_OP_STRING_MAP);
        ProfiledMethodFactory factory = new ProfiledMethodFactory(profile);
        
        factory.getProfiledMethod(getTracer(new ClassMethodSignature("Foo", "bar", "(Ljava/lang/String;)V")));
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out);
        JSONObject.writeJSONString(factory.getMethods(), writer);
        writer.close();
        
        Assert.assertEquals("{\"0\":[\"Foo\",\"bar\",-1,[{\"args\":[\"java.lang.String\"]}]]}", out.toString());
    }

    private Tracer getTracer(ClassMethodSignature signature) {
        Tracer tracer = Mockito.mock(Tracer.class);
        Mockito.when(tracer.getClassMethodSignature()).thenReturn(signature);
        return tracer;
    }
}
