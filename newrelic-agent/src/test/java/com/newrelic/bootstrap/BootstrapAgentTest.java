package com.newrelic.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;

public class BootstrapAgentTest {
    @Test
    public void decodeAndDecompressAgentArguments_withProperInput_returnsUncompressedString() throws IOException {
        // Compress and Base64 encode agent arg string
        String fakeAgentArgs = "this is a fake agent arg string";
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream outputStream = new DeflaterOutputStream(arrayOutputStream);
        for(byte b: fakeAgentArgs.getBytes(StandardCharsets.UTF_8)){
            outputStream.write(b);
        }
        outputStream.close();

        String result = BootstrapAgent.decodeAndDecompressAgentArguments(Base64.getEncoder().encodeToString(arrayOutputStream.toByteArray()));
        Assert.assertEquals(fakeAgentArgs, result);
    }
}
