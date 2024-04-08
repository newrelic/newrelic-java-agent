/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package netty4116;

import com.agent.instrumentation.netty4116.RequestWrapper;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestWrapperTest {
    @Test
    public void testPercentageEscaping() {
        Map<String, String> inputExpectedMap = new HashMap<>();
        inputExpectedMap.put("http://example.com?asdf=%qwer", "%qwer");
        inputExpectedMap.put("http://example.com?asdf=%20", " ");
        inputExpectedMap.put("http://example.com?asdf=%2b", "+");
        inputExpectedMap.put("http://example.com?asdf=qwer", "qwer");

        for (Map.Entry<String, String> inputExpectedEntry : inputExpectedMap.entrySet()) {
            String input = inputExpectedEntry.getKey();
            String expected = inputExpectedEntry.getValue();
            HttpRequest request = mock(HttpRequest.class);
            when(request.headers()).thenReturn(EmptyHttpHeaders.INSTANCE);
            when(request.uri()).thenReturn(input);
            RequestWrapper requestWrapper = new RequestWrapper(request);
            Assert.assertEquals(expected, requestWrapper.getParameterValues("asdf")[0]);
        }
    }
}
