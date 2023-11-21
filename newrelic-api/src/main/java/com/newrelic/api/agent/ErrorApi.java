package com.newrelic.api.agent;

import java.util.Collections;
import java.util.Map;

public interface ErrorApi {
    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param throwable The throwable to notice and report.
     * @param params Custom parameters to include in the traced error. May be null.
     * @since 1.3.0
     */
    default void noticeError(Throwable throwable, Map<String, ?> params) {
        noticeError(throwable, params, false);
    }

    /**
     * Report an exception to New Relic.
     *
     * @param throwable The throwable to report.
     * @see #noticeError(Throwable, Map)
     * @since 1.3.0
     */
    default void noticeError(Throwable throwable) {
        noticeError(throwable, Collections.emptyMap(), false);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param message The error message to be reported.
     * @param params Custom parameters to include in the traced error. May be null.
     * @since 1.3.0
     */
    default void noticeError(String message, Map<String, ?> params) {
        noticeError(message, params, false);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * @param message Message to report with a transaction when it finishes.
     * @since 2.21.0
     */
    default void noticeError(String message) {
        noticeError(message, Collections.emptyMap(), false);
    }

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     * <p>
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param throwable The throwable to notice and report.
     * @param params    Custom parameters to include in the traced error. May be null.
     * @param expected  true if this error is expected, false otherwise.
     * @since 3.38.0
     */
    void noticeError(Throwable throwable, Map<String, ?> params, boolean expected);

    /**
     * Report an exception to New Relic.
     * <p>
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param throwable The throwable to report.
     * @param expected  true if this error is expected, false otherwise.
     * @see #noticeError(Throwable, Map)
     * @since 3.38.0
     */
    default void noticeError(Throwable throwable, boolean expected) {
        noticeError(throwable, Collections.emptyMap(), expected);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     * <p>
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * <p>
     * <b>Note:</b> The key and value pairs in custom parameters {@code params} will be dropped or modified in the
     * traced error if the key or value, each, cannot be encoded in 255 bytes. If key or value is over this limit, the
     * behavior will be the same as defined in {@link #addCustomParameter(String key, String value) addCustomParameter}.
     * </p>
     *
     * @param message  The error message to be reported.
     * @param params   Custom parameters to include in the traced error. May be null.
     * @param expected true if this error is expected, false otherwise.
     * @since 3.38.0
     */
    void noticeError(String message, Map<String, ?> params, boolean expected);

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic. If noticeError is invoked multiple times while in a transaction, only the
     * last error will be reported.
     *
     * Expected errors do not increment an application's error count or contribute towards its Apdex score.
     *
     * @param message Message to report with a transaction when it finishes.
     * @param expected true if this error is expected, false otherwise.
     * @since 3.38.0
     */
    default void noticeError(String message, boolean expected) {
        noticeError(message, Collections.emptyMap(), expected);
    }

    /**
     * Registers an {@link ErrorGroupCallback} that's used to generate a grouping key for the supplied
     * error. This key will be used to group similar error messages on the Errors Inbox UI. If the
     * errorGroupCallback instance is null no grouping key will be generated.
     *
     * @param errorGroupCallback the ErrorGroupCallback used to generate grouping keys for errors
     * @since 8.10.0
     */
    void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback);
}
