package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;
import reactor.core.CoreSubscriber;
import reactor.util.annotation.Nullable;

@Weave(originalName = "reactor.core.publisher.MonoCreate")
class MonoCreate_Instrumentation {

    @Weave(originalName = "reactor.core.publisher.MonoCreate$DefaultMonoSink")
    static final class DefaultMonoSink_Instrumentation<T> {

        @NewField
        private Token token;

        DefaultMonoSink_Instrumentation(CoreSubscriber<? super T> actual) {
            this.token = NewRelic.getAgent().getTransaction().getToken();
        }

        @Trace(async = true)
        public void cancel() {
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true)
        public void error(Throwable e) {
            if(ReactorConfig.errorsEnabled) {
                NewRelic.noticeError(e);
            }
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true)
        public void success() {
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true)
        public void success(T value) {
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async = true)
        public void request(long n) {
            if(token != null) {
                token.link();
            }
            Weaver.callOriginal();
        }

    }

}
