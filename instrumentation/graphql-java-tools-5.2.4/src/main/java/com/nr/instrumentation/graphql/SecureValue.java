package com.nr.instrumentation.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import graphql.schema.GraphQLScalarType;

public class SecureValue {

    public final static GraphQLScalarType ScalarType =
            GraphQLScalarType.newScalar()
                    .name("SecureValue")
                    .coercing(new SecureValueCoercing())
                    .build();

    private final String value;

    @JsonCreator
    public SecureValue(@JsonProperty("value") String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    /**
     * Returns the value elideed in the middle with `...` (three dots).
     * It will keep up to <code>keepCharCount/2</code> characters of the original string on each end,
     * rounded down. Example: if <code>keepCharCount</code> is 3, we will keep 1 character (floor(3/2)) on each side.
     *
     * <code>keepCharCount</code> should never be greater then or equal to
     * <code>value.length()</code>, or smaller then or equal to 1, otherwise an
     * exception is raised.
     *
     * @param keepCharCount the amount of characters in the value that you want to be elided
     * @return String with the value elideed in the middle
     * @throws SecureValueElisionException
     */
    public String getElidedValue(int keepCharCount) throws SecureValueElisionException {
        if (keepCharCount <= 1 || keepCharCount >= value.length()) {
            throw new SecureValueElisionException(
                    String.format(
                            "Cannot elide %d characters from value with %d characters. Minimum is %d and maximum is %d.",
                            keepCharCount,
                            value.length(),
                            2,
                            value.length() - 2
                    )
            );
        }
        int halfSize = (int) Math.floor(keepCharCount/2.0);
        return value.substring(0, halfSize) +
                "..." +
                value.substring(value.length() - halfSize);
    }

    @Override
    public String toString() {
        return "SecureValue{"
                + "value=" + value
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SecureValue other = (SecureValue) o;

        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static class SecureValueElisionException extends IllegalArgumentException {
        public SecureValueElisionException(String format) {
            super(format);
        }
    }
}