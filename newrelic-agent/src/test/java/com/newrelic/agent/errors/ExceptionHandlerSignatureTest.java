/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionHandlerSignatureTest {

    @Test(expected = InvalidMethodDescriptor.class)
    public void badSignature() throws InvalidMethodDescriptor {
        ExceptionHandlerSignature sig = new ExceptionHandlerSignature("com/test/Dude", "handle",
                "(java/lang/String;java/lang/Exception;)V");
    }

    @Test
    public void exception() throws InvalidMethodDescriptor {
        ExceptionHandlerSignature sig = new ExceptionHandlerSignature("com/test/Dude", "handle",
                "(Ljava/lang/String;Ljava/lang/Exception;)V");
        Assert.assertEquals(1, sig.getExceptionArgumentIndex());
    }

    @Test
    public void throwable() throws InvalidMethodDescriptor {
        ExceptionHandlerSignature sig = new ExceptionHandlerSignature("com/test/Dude", "handle",
                "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;)V");
        Assert.assertEquals(0, sig.getExceptionArgumentIndex());
    }

    @Test
    public void error() throws InvalidMethodDescriptor {
        ExceptionHandlerSignature sig = new ExceptionHandlerSignature("com/test/Dude", "handle",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Error;)V");
        Assert.assertEquals(2, sig.getExceptionArgumentIndex());
    }

    @Test
    public void servletException() throws InvalidMethodDescriptor {
        ExceptionHandlerSignature sig = new ExceptionHandlerSignature("com/test/Dude", "handle",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Runnable;Ljavax/servlet/ServletException;)V");
        Assert.assertEquals(3, sig.getExceptionArgumentIndex());
    }

    @Test
    public void specialException() throws InvalidMethodDescriptor {
        ExceptionHandlerSignature sig = new ExceptionHandlerSignature("com/test/Dude", "handle",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Runnable;Ljava/lang/RuntimeException;)V");
        Assert.assertEquals(-1, sig.getExceptionArgumentIndex());
    }
}
