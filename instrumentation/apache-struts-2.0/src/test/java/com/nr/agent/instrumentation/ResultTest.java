package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "com.opensymphony.xwork2" })
public class ResultTest {
    @Test
    public void execute_SetsSegmentName() throws Exception {
        SampleStruts2Result result = new ResultTest.SampleStruts2Result();

        StrutsSampleApp.executeOnResult(result);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Assert.assertTrue(introspector.getMetricsForTransaction("OtherTransaction/Custom/com.nr.agent.instrumentation.StrutsSampleApp/executeOnResult")
                .containsKey("StrutsResult/java.util.ArrayList"));
    }

    public static class SampleStruts2Result implements Result {
        @Override
        public void execute(ActionInvocation invocation) throws Exception {
        }
    }
}
