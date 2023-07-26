package com.newrelic.agent.service.module;

import org.junit.Test;

import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JarDataTest {

    @Test
    public void testWriteJSONString() throws IOException {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("tiny", "tambourines");
        attributes.put("giant", "giraffes");
        JarInfo info = new JarInfo("2.0.0", attributes);
        JarData data = new JarData("benny", info);

        Writer writer = new StringWriter();
        data.writeJSONString(writer);
        String expected = "[\"benny\",\"2.0.0\",{\"tiny\":\"tambourines\",\"giant\":\"giraffes\"}]";
        assertEquals(expected, writer.toString());
    }

    @Test
    public void testEqualsAndHashCode(){
        Map<String, String> attributes = new HashMap<>();
        attributes.put("tiny", "tambourines");
        attributes.put("giant", "giraffes");
        JarInfo info = new JarInfo("2.0.0", attributes);
        JarInfo infoDifferentVersion = new JarInfo("1.0.0", attributes);
        JarInfo infoDifferentAttributes = new JarInfo("2.0.0", null);

        JarData data1 = new JarData("arthur", info);
        JarData data2 = new JarData("benny", info);
        JarData data3 = new JarData("arthur", infoDifferentVersion);
        JarData data4 = new JarData("arthur", infoDifferentAttributes);
        JarData missingName = new JarData(null, info);

        FakeJarData fakeData = new FakeJarData("arthur", info);

        assertEquals(data1, data1);

        assertNotEquals(data1, data2);
        assertNotEquals(data2, data1);
        assertNotEquals(data1.hashCode(), data2.hashCode());

        assertNotEquals(data1, data3);
        assertNotEquals(data3, data1);
        assertNotEquals(data1.hashCode(), data3.hashCode());

        assertEquals(data1, data4);
        assertEquals(data4, data1);
        assertEquals(data1.hashCode(), data4.hashCode());

        assertNotEquals(data1, missingName);
        assertNotEquals(missingName, data1);
        assertNotEquals(data1.hashCode(), missingName.hashCode());

        assertNotEquals(data1, fakeData);
        assertNotEquals(data1, null);
    }

    private class FakeJarData {

        String name;
        JarInfo info;
        public FakeJarData(String name, JarInfo info) {
            this.name = name;
            this.info = info;
        }
    }

}