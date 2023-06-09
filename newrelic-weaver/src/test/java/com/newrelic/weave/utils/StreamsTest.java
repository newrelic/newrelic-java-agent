package com.newrelic.weave.utils;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.IOException;

public class StreamsTest {

    @Test
    public void test_getClassBytes_success() throws IOException {
        byte[] result = Streams.getClassBytes(StreamsTest.class);
        Assert.assertTrue(new String(result).contains("com/newrelic/weave/utils/StreamsTest"));
    }

    @Test
    public void test_getClassBytes_nullClassLoaderGoodClass() throws IOException {
        byte[] result = Streams.getClassBytes(null, Object.class.getName().replace('.', '/'));
        Assert.assertTrue(new String(result).contains("java/lang/Object"));
    }

    @Test
    public void test_getClassBytes_nullClassLoaderBadClass() throws IOException {
        byte[] result = Streams.getClassBytes(null, StreamsTest.class.getName().replace('.', '/'));
        Assert.assertEquals(null, result);
    }
}
