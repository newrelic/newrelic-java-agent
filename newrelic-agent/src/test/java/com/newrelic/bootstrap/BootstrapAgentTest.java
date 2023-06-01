package com.newrelic.bootstrap;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class BootstrapAgentTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    public void setUpStreams() {
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testDecodeAndDecompressAgentArguments() throws IOException {
        String originalString = "This is a test string";
        String encodedCompressedString = compressAndEncode(originalString);

        String result = BootstrapAgent.decodeAndDecompressAgentArguments(encodedCompressedString);
        assertEquals(originalString, result);
    }

    @Test
    void testDecodeAndDecompressAgentArguments_invalidInput() {
        String invalidString = "This is an invalid string";
        assertThrows(IOException.class, () -> {
            BootstrapAgent.decodeAndDecompressAgentArguments(invalidString);
        });
    }

    @Test
    void testDecodeAndDecompressAgentArguments_emptyInput() {
        String emptyString = "";
        assertThrows(IOException.class, () -> {
            BootstrapAgent.decodeAndDecompressAgentArguments(emptyString);
        });
    }

    private String compressAndEncode(String input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream);
        deflaterOutputStream.write(input.getBytes());
        deflaterOutputStream.close();
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    @Test
    public void testIsJavaSqlLoadedOnPlatformClassLoader() {
        assertTrue(BootstrapAgent.isJavaSqlLoadedOnPlatformClassLoader("11"));
        assertFalse(BootstrapAgent.isJavaSqlLoadedOnPlatformClassLoader("1.8"));

    }

    @Test
    public void testPrintExperimentalRuntimeModeInUseMessage() {
        BootstrapAgent.printExperimentalRuntimeModeInUseMessage("11");
        String output = outContent.toString();
        assertTrue(output.contains("Experimental runtime mode is enabled."));
    }

    @Test
    public void testPrintUnsupportedJavaVersionMessage() {
        BootstrapAgent.printUnsupportedJavaVersionMessage("11");
        String output = errContent.toString();
        assertTrue(output.contains("Unsupported agent Java spec version"));
    }




}
