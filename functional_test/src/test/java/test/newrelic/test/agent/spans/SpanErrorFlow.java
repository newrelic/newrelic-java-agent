/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.spans;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;

public abstract class SpanErrorFlow {
    @Trace(dispatcher = true)
    public void transactionLaunchPoint() throws InterruptedException {
        Thread.sleep(2);
        possiblyHandlingMethod();
        Thread.sleep(2);
    }

    @Trace(dispatcher = true)
    public void webLaunchPoint(final int status) throws InterruptedException {
        NewRelic.getAgent().getTransaction().setWebResponse(new ErrorExtendedResponse(status, this.getClass().getName()));
        Thread.sleep(2);
        possiblyHandlingMethod();
        Thread.sleep(2);
    }

    @Trace
    protected void possiblyHandlingMethod() throws InterruptedException {
        Thread.sleep(3);
        intermediatePassThroughMethod();
        Thread.sleep(3);
    }

    @Trace
    protected void intermediatePassThroughMethod() throws InterruptedException {
        Thread.sleep(1);
        activeMethod();
        Thread.sleep(1);
    }

    @Trace
    protected abstract void activeMethod() throws InterruptedException;

    public static class Nothing extends SpanErrorFlow {
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
        }
    }

    public static class ThrowEscapingException extends SpanErrorFlow {
        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            throw new RuntimeException("~~ oops ~~");
        }
    }

    public static class NoticeErrorString extends SpanErrorFlow {
        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            NewRelic.noticeError("~~ noticed string ~~");
        }
    }

    public static class NoticeErrorException extends SpanErrorFlow {
        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            NewRelic.noticeError(new Exception("~~ noticed ~~"));
        }
    }

    public static class NoticeAndThrow extends SpanErrorFlow {
        @SuppressWarnings({ "NumericOverflow", "divzero" })
        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            NewRelic.noticeError("noticed, not thrown");
            System.out.println(1 / 0);
        }
    }

    public static class ThrowNoticeAndSquelch extends SpanErrorFlow {
        @Trace
        @Override
        protected void possiblyHandlingMethod() {
            try {
                super.possiblyHandlingMethod();
            } catch (Throwable t) {
                NewRelic.noticeError(t);
            }
        }

        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            throw new RuntimeException("~~ oops ~~");
        }
    }

    public static class CatchAndRethrow extends SpanErrorFlow {
        @Trace
        @Override
        protected void possiblyHandlingMethod() {
            try {
                Thread.sleep(3);
                intermediatePassThroughMethod();
                Thread.sleep(3);
            } catch (Throwable t) {
                throw new CustomFooException("~~ caught ~~", t);
            }
        }

        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            throw new RuntimeException("~~ oops ~~");
        }
    }

    public static class ThrowHandledException extends SpanErrorFlow {
        @Trace
        @Override
        protected void possiblyHandlingMethod() {
            try {
                Thread.sleep(3);
                intermediatePassThroughMethod();
                Thread.sleep(3);
            } catch (Throwable ignored) {
            }
        }

        @Trace
        @Override
        protected void activeMethod() throws InterruptedException {
            Thread.sleep(1);
            throw new RuntimeException("~~ oops ~~");
        }
    }
}
