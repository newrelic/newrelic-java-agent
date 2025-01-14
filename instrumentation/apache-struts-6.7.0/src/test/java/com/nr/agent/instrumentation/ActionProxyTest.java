package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.DefaultActionProxy;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import org.apache.struts2.ActionProxy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.struts2" })
public class ActionProxyTest {
    @Test
    public void execute_setsTxnName() throws Exception {
        ActionProxy proxy = new ActionProxyTest.SampleStruts2ActionProxy(new MockActionInvocation(), "namespace", "actionName", "method", true, true);

        StrutsSampleApp.executeOnActionProxy(proxy);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getTransactionNames().contains("OtherTransaction/StrutsAction/actionName"));
    }

    public static class SampleStruts2ActionProxy extends DefaultActionProxy {

        public SampleStruts2ActionProxy(ActionInvocation inv, String namespace, String actionName, String methodName, boolean executeResult,
                boolean cleanupContext) {
            super(inv, namespace, actionName, methodName, executeResult, cleanupContext);
        }


    }
}
