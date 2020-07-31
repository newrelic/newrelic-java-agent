/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.newrelic.weave.utils.Streams;

public class ShaChecksums {

    private ShaChecksums() {
    }

    public static String computeSha(URL url) throws NoSuchAlgorithmException, IOException {
        return computeSha(EmbeddedJars.getInputStream(url));
    }
    
    public static String computeSha(File file) throws NoSuchAlgorithmException, IOException {
        return computeSha(new FileInputStream(file));
    }

    public static String computeSha(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        return computeSha(inputStream, "SHA1");
    }
    
    public static String computeSha512(URL url) throws NoSuchAlgorithmException, IOException {
        return computeSha512(EmbeddedJars.getInputStream(url));
    }
    
    private static String computeSha512(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        return computeSha(inputStream, "SHA-512");
    }

    private static String computeSha(InputStream inputStream, String algorithm) throws NoSuchAlgorithmException, IOException {

        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);

            DigestInputStream dis = new DigestInputStream(inputStream, md);
            byte[] buffer = new byte[Streams.DEFAULT_BUFFER_SIZE];
            // read in the stream in chunks while updating the digest
            while (dis.read(buffer) != -1) {
            }

            byte[] mdbytes = md.digest();

            // convert to hex format
            StringBuffer sb = new StringBuffer(40);
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } finally {
            inputStream.close();
        }
    }

}
