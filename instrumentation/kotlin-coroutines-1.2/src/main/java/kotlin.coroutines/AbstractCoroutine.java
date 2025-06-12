package kotlinx.coroutines;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines.Utils;

import kotlin.coroutines.CoroutineContext;

@Weave(type=MatchType.BaseClass)
public abstract class AbstractCoroutine<T> {

	public abstract CoroutineContext getContext();
	public abstract String nameString$kotlinx_coroutines_core();

	protected void onCompleted(T value) {
		Utils.expireToken(getContext());
		Weaver.callOriginal();
	}

	protected void onCancelled(Throwable t, boolean b) {
		Utils.expireToken(getContext());
		Weaver.callOriginal();
	}

	public void handleOnCompletionException$kotlinx_coroutines_core(Throwable t) {
		Utils.expireToken(getContext());
		Weaver.callOriginal();
	}

}
