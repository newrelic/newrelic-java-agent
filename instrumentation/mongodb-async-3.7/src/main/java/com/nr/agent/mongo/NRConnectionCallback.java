/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.mongo;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.binding.AsyncConnectionSource;
import com.mongodb.connection.ServerDescription;

public class NRConnectionCallback implements SingleResultCallback<AsyncConnectionSource> {

    private String host = null;
    private Integer port = null;

    @Override
    public void onResult(AsyncConnectionSource result, Throwable t) {
        ServerDescription serverDesc = result.getServerDescription();
        host = serverDesc.getAddress().getHost();
        port = serverDesc.getAddress().getPort();
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
