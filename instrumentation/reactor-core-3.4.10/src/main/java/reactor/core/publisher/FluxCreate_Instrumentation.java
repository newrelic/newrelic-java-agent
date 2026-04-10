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
class FluxCreate_Instrumentation<T> {

    @Weave(originalName = "reactor.core.publisher.FluxCreate$BaseSink", type = MatchType.BaseClass)
    static abstract class BaseSink_Instrumentation<T> {

        @NewField
        protected Token token;

        BaseSink_Instrumentation(CoreSubscriber<? super T> actual) {
            token = NewRelic.getAgent().getTransaction().getToken();
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
            if(ReactorConfig.errorsEnabled) {
                NewRelic.noticeError(t);
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
    static final class BufferAsyncSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {

        BufferAsyncSink_Instrumentation(CoreSubscriber<? super T> actual, int capacityHint) {
            super(actual);
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if (token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }

    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$IgnoreSink")
    static final class IgnoreSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {

        IgnoreSink_Instrumentation(CoreSubscriber<? super T> actual) {
            super(actual);
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if (token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$NoOverflowBaseAsyncSink", type = MatchType.BaseClass)
    static abstract class NoOverflowBaseAsyncSink_Instrumentation<T> extends BaseSink_Instrumentation<T> {

        NoOverflowBaseAsyncSink_Instrumentation(CoreSubscriber<? super T> actual) {
            super(actual);
        }

        @Trace(async = true, excludeFromTransactionTrace = true)
        public FluxSink<T> next(T t) {
            if (token != null) {
                token.link();
            }
            return Weaver.callOriginal();
        }
    }

    @Weave(originalName = "reactor.core.publisher.FluxCreate$SerializedFluxSink")
    static final class FluxCreate$SerializedFluxSink_Instrumentation<T> {

        @NewField
        private Token token;

        FluxCreate$SerializedFluxSink_Instrumentation(BaseSink_Instrumentation<T> sink) {
            if(sink != null) {
                if(sink.token != null) {
                    token = sink.token;
                }
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

    @Weave(originalName = "reactor.core.publisher.FluxCreate$SerializeOnRequestSink")
    static class SerializeOnRequestSink_Instrumentation<T> {

        @NewField
        private Token token;

        SerializeOnRequestSink_Instrumentation(BaseSink_Instrumentation<T> sink) {
            if(sink != null) {
                if(sink.token != null) {
                    token = sink.token;
                }
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
