package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;
import reactor.core.CoreSubscriber;

@Weave(originalName = "reactor.core.publisher.FluxCreate")
class FluxCreate_Instrumentation {

    @Weave(originalName = "reactor.core.publisher.FluxCreate$BaseSink", type = MatchType.BaseClass)
    static abstract class BaseSink_Instrumentation<T> implements FluxSink<T> {

        @NewField
        protected Token token = null;

        BaseSink_Instrumentation(CoreSubscriber<? super T> actual) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }

        @Trace(async=true, excludeFromTransactionTrace = true)
        public void complete() {

            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async=true, excludeFromTransactionTrace = true)
        public void error(Throwable e) {
            if(ReactorConfig.errorsEnabled) {
                NewRelic.noticeError(e);
            }
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        public void cancel() {
            if (token != null) {
                token.expire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void request(long n) {
            if (token != null) {
                token.link();
            }
            Weaver.callOriginal();
        }

    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$BufferAsyncSink")
    static abstract class BufferAsyncSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {
        BufferAsyncSink_Instrumentation(CoreSubscriber<? super T> actual, int capacityHint) {
            super(actual);
        }

        @Trace(async=true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if(token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$IgnoreSink")
    static abstract class IgnoreSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {
        IgnoreSink_Instrumentation(CoreSubscriber<? super T> actual) {
            super(actual);
        }

        @Trace(async=true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if(token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$LatestAsyncSink")
    static abstract class LatestAsyncSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {
        LatestAsyncSink_Instrumentation(CoreSubscriber<? super T> actual) {
            super(actual);
        }

        @Trace(async=true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if(token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$NoOverflowBaseAsyncSink", type =  MatchType.BaseClass)
    static abstract class NoOverflowBaseAsyncSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {
        NoOverflowBaseAsyncSink_Instrumentation(CoreSubscriber<? super T> actual) {
            super(actual);
        }

        @Trace(async=true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if(token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$SerializeOnRequestSink")
    static class SerializeOnRequestSink_Instrumentation<T> {

        @NewField
        protected Token token;

        SerializeOnRequestSink_Instrumentation(BaseSink_Instrumentation<T> sink) {
            if(sink != null) {
                token = sink.token;
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void complete() {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void error(Throwable t) {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if (token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }

    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$SerializedSink")
    static class SerializedSink_Instrumentation<T> {

        @NewField
        protected Token token;

        SerializedSink_Instrumentation(BaseSink_Instrumentation<T> sink) {
            if(sink != null) {
                token = sink.token;
            }
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void complete() {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public void error(Throwable t) {
            if (token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if (token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }

    }

}
