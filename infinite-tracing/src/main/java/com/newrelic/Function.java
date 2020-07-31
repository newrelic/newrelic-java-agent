package com.newrelic;

/**
 * Backport of `java.util.function.Function` until we can make this Java 8+.
 */
public interface Function<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t);
}
