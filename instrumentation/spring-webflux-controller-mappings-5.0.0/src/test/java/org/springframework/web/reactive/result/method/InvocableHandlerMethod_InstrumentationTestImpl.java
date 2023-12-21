package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;

public class InvocableHandlerMethod_InstrumentationTestImpl extends InvocableHandlerMethod_Instrumentation {
    private final Class<?> beanType;
    private final Method bridgedMethod;

    public InvocableHandlerMethod_InstrumentationTestImpl(Class<?> beanType, Method bridgedMethod) {
        this.beanType = beanType;
        this.bridgedMethod = bridgedMethod;
    }

    @Override
    protected Method getBridgedMethod() {
        return bridgedMethod;
    }

    @Override
    public Class<?> getBeanType() {
        return beanType;
    }
}
