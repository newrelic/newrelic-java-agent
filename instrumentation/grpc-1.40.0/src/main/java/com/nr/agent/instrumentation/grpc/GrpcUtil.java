/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.grpc;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import io.grpc.Metadata;
import io.grpc.Status;

public class GrpcUtil {

    /**
     * Finalize the transaction when a Stream closed or is cancelled by linking the supplied token,
     * setting the appropriate headers and marking the transaction as sent.
     *
     * @param token The {@link Token} to expire
     * @param status The {@link Status} of the completed/cancelled operation
     * @param metadata Operation {@link Metadata} to be included with the transaction
     */
    public static void finalizeTransaction(Token token, Status status, Metadata metadata) {
        if (token != null) {
            token.link();
            Transaction transaction = NewRelic.getAgent().getTransaction();
            transaction.setWebResponse(new GrpcResponse(status, metadata));
            transaction.addOutboundResponseHeaders();
            transaction.markResponseSent();
        }
    }

    /**
     * Set the response.status attribute and error cause (if applicable) on the transaction
     * when the ServerStream is closed or cancelled.
     *
     * @param status The {@link Status} of the completed/cancelled operation
     */
    public static void setServerStreamResponseStatus(Status status) {
        if (status != null) {
            NewRelic.addCustomParameter("response.status", status.getCode().value());
            if (GrpcConfig.errorsEnabled && status.getCause() != null) {
                // If an error occurred during the close of this server call we should record it
                NewRelic.noticeError(status.getCause());
            }
        }
    }
}
