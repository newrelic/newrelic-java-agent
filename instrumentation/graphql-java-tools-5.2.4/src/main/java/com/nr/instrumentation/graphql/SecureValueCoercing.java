package com.nr.instrumentation.graphql;

public class SecureValueCoercing extends StringCoercing<SecureValue> {

    @Override
    protected SecureValue parseFromString(String input) {
        return new SecureValue(input);
    }

    @Override
    protected String serializeToString(SecureValue input) {
        return input.getValue();
    }

}