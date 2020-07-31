/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import fi.iki.elonen.NanoWSD;

import java.io.IOException;

public class EchoServer extends NanoWSD {
    public EchoServer(int port) {
        super(port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new EchoHandler(handshake);
    }

    public static class EchoHandler extends NanoWSD.WebSocket {
        public EchoHandler(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
            message.setUnmasked();
            try {
                sendFrame(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
        }
    }
}
