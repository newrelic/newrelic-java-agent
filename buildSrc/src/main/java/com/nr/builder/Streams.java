/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 8;

    private Streams() {
    }

    static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[Streams.DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
