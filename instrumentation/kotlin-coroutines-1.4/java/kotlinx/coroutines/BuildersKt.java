package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.kotlin.coroutines.NRCoroutineToken;
import com.newrelic.instrumentation.kotlin.coroutines.NRFunction2Wrapper;
import com.newrelic.instrumentation.kotlin.coroutines.Utils;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Weave
public class BuildersKt {

	@Trace
	public static final <T> T runBlocking(CoroutineContext context, Function2<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> block) {
		
		if (!Utils.ignoreSuspend(block.getClass(), context)) {
			if (!(block instanceof NRFunction2Wrapper)) {
				String name = Utils.getCoroutineName(context);
				if (name == null)
					name = block.getClass().getName();
				NRFunction2Wrapper<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction2Wrapper(
						block, name);
				block = wrapper;
			} 
		}
		T t = Weaver.callOriginal();
		return t;
	}

	@Trace
	public static final <T> Deferred<T> async(CoroutineScope scope, CoroutineContext context, CoroutineStart cStart, Function2<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> block) {
		if(!Utils.ignoreSuspend(block.getClass(),context) && !Utils.ignoreSuspend(block.getClass(), scope.getCoroutineContext())) {
			NRCoroutineToken nrContextToken = Utils.setToken(context);
			if(nrContextToken != null) {
				context = context.plus(nrContextToken);
			}
			if(!(block instanceof NRFunction2Wrapper)) {
				String name = Utils.getCoroutineName(context);
				if(name == null) {
					name = Utils.getCoroutineName(scope.getCoroutineContext());
				}
				if(name == null) name = block.getClass().getName();
				NRFunction2Wrapper<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction2Wrapper(block,name);
				block = wrapper;
			}
		}
		return Weaver.callOriginal();
	}

	@Trace
	public static final <T> Object invoke(CoroutineDispatcher dispatcher, Function2<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> block, Continuation<? super T> c) {
		if(!Utils.ignoreSuspend(block.getClass(), null)) {
			if(!(block instanceof NRFunction2Wrapper)) {
				NRFunction2Wrapper<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction2Wrapper(block,block.getClass().getName());
				block = wrapper;
			}
		}
		Object t = Weaver.callOriginal();
		return t;
	}

	@Trace
	public static final kotlinx.coroutines.Job launch(CoroutineScope scope, CoroutineContext context, CoroutineStart cStart, Function2<? super CoroutineScope, ? super Continuation<? super Unit>, ? extends Object> block) {
		if(!Utils.ignoreSuspend(block.getClass(), context) && !Utils.ignoreSuspend(block.getClass(), scope.getCoroutineContext())) {
			NRCoroutineToken nrContextToken = Utils.setToken(context);
			if(nrContextToken != null) {
				context = context.plus(nrContextToken);
			}
			if(!(block instanceof NRFunction2Wrapper)) {
				String name = Utils.getCoroutineName(context);
				if(name == null) {
					name = Utils.getCoroutineName(scope.getCoroutineContext());
				}
				if(name == null) name = block.getClass().getName();
				NRFunction2Wrapper<? super CoroutineScope, ? super Continuation<? super Unit>, ? extends Object> wrapper = new NRFunction2Wrapper(block,name);
				block = wrapper;
			}
		}
		Job j = Weaver.callOriginal();
		return j;
	}

	@Trace
	public static final <T> Object withContext(CoroutineContext context,Function2<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> block, Continuation<? super T> completion) {
		if(!Utils.ignoreSuspend(block.getClass(),context)) {

			NRCoroutineToken nrContextToken = Utils.setToken(context);
			if(nrContextToken != null) {
				context = context.plus(nrContextToken);
			}
			if(!(block instanceof NRFunction2Wrapper)) {
				String name = Utils.getCoroutineName(context);
				if(name == null) name = block.getClass().getName();
				NRFunction2Wrapper<? super CoroutineScope, ? super Continuation<? super T>, ? extends Object> wrapper = new NRFunction2Wrapper(block,name);
				block = wrapper;
			}
		}
		return Weaver.callOriginal();
	}
}