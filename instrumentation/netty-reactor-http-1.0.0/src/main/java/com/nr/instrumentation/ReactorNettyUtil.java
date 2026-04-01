package com.nr.instrumentation;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

/**
 * Utility methods for Reactor Netty instrumentation.
 */
public final class ReactorNettyUtil {

    /**
     * Checks if the current transaction is a NoOp transaction.
     * NoOp transactions should not be instrumented.
     *
     * @param transaction the transaction to check
     * @return true if the transaction is a NoOp transaction or null
     */
    public static boolean isNoOpTransaction(Transaction transaction) {
        if (transaction == null) {
            return true;
        }

        // Check if transaction class name contains "NoOp"
        String className = transaction.getClass().getName();
        return className.contains("NoOp");
    }

    /**
     * Safely parses a URI string, logging any errors.
     *
     * @param uriString the URI string to parse
     * @return the parsed URI, or null if parsing fails
     */
    public static URI safeParseURI(String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            return null;
        }

        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e,
                "Failed to parse URI: {0}", uriString);
            return null;
        }
    }

}