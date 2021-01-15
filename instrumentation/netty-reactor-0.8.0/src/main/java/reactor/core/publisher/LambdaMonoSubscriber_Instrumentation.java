//package reactor.core.publisher;
//
//import com.newrelic.agent.bridge.AgentBridge;
//import com.newrelic.api.agent.NewRelic;
//import com.newrelic.api.agent.Token;
//import com.newrelic.api.agent.weaver.NewField;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.WeaveAllConstructors;
//import com.newrelic.api.agent.weaver.Weaver;
//import reactor.core.CoreSubscriber;
//import reactor.util.context.Context;
//
//@Weave(originalName = "reactor.core.publisher.LambdaMonoSubscriber")
//abstract class LambdaMonoSubscriber_Instrumentation  {
//    @NewField
//    private  Context nrContext;
//
//    //final Context initialContext = Weaver.callOriginal();
//
//    @WeaveAllConstructors
//    protected LambdaMonoSubscriber_Instrumentation() {
//        if (AgentBridge.getAgent().getTransaction(false) != null) {
//            nrContext = Context.of("newrelic-token", NewRelic.getAgent().getTransaction().getToken());
//        }
//        //this.initialContext = Weaver.callOriginal();
//;
//    }
//
////        public final void onSubscribe(Subscription s) {
////        Weaver.callOriginal();
////        //((FluxContextWrite.ContextWriteSubscriber) ((FluxOnAssembly.OnAssemblySubscriber) subscription).s).currentContext();
////
////    }
//    	public final void onComplete() {
//            Token token = this.currentContext().getOrDefault("newrelic-token", null);
//            if (token != null) {
//                token.expire();
//                token = null;
//                this.nrContext = null;
//
//            }
//        }
//
//    public Context currentContext() {
////        AgentBridge.activeToken.get();
//        if (nrContext != null) {
//           return nrContext;
//            //return initialContext.putAll(nrContext);
//        }
//        return Weaver.callOriginal();
//    }
//
//}
