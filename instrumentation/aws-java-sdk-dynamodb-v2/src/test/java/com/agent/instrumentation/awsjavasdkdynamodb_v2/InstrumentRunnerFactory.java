package com.agent.instrumentation.awsjavasdkdynamodb_v2;

import com.newrelic.agent.introspec.InstrumentationTestRunnerWithParameters;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

public class InstrumentRunnerFactory implements ParametersRunnerFactory {

    @Override
    public Runner createRunnerForTestWithParameters(TestWithParameters testWithParameters) throws InitializationError {
        try {
            return new InstrumentationTestRunnerWithParameters(testWithParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
