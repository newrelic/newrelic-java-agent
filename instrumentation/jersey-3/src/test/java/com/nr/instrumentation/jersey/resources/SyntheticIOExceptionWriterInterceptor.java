/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.instrumentation.jersey.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

/**
 * This WriterInterceptor simulates an IOException occurring when writing out a response to a client. This reproduces a
 * bug in our instrumentation where a Segment is created but never finished, preventing the Transaction from finishing.
 */
@Provider
@SyntheticIOException
public class SyntheticIOExceptionWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        throw new IOException("Simulated IOException when writing Response to client");
    }

}
