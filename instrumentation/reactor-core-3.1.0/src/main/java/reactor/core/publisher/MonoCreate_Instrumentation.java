package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;
import reactor.core.CoreSubscriber;

@Weave(originalName = "reactor.core.publisher.MonoCreate")
class MonoCreate_Instrumentation {

    @Weave(originalName = "reactor.core.publisher.MonoCreate$DefaultMonoSink")
    static class DefaultMonoSink_Instrumentation<T> {

        @NewField
        private Token token = null;

        DefaultMonoSink_Instrumentation(CoreSubscriber<? super T> actual) {
            Token t = NewRelic.getAgent().getTransaction().getToken();
            if(t != null && t.isActive()) {
                token = t;
            } else if(t != null) {
                t.expire();
                t = null;
            }
        }

        @Trace(async=true)
        public void success() {
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async=true)
        public void success(T value) {
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }

        @Trace(async=true)
        public void error(Throwable e) {
            if(ReactorConfig.errorsEnabled || ReactorConfig.errorsEnabledNetty) {
                NewRelic.noticeError(e);
            }
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            NewRelic.noticeError(e);
            Weaver.callOriginal();
        }

        @Trace(async=true)
        public void cancel() {
            if(token != null) {
                token.linkAndExpire();
                token = null;
            }
            Weaver.callOriginal();
        }
    }

}
