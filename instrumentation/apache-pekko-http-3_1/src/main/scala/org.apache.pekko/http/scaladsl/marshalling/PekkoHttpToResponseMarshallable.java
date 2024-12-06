/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.pekko.http.scaladsl.marshalling;

import org.apache.pekko.http.scaladsl.model.HttpResponse;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "org.apache.pekko.http.scaladsl.marshalling.ToResponseMarshallable")
public abstract class PekkoHttpToResponseMarshallable {

    @NewField
    public Token token;

    public Marshaller<Object, HttpResponse> marshaller() {
        Marshaller<Object, HttpResponse> marshaller = Weaver.callOriginal();
        PekkoHttpMarshallerMapper pekkoHttpMarshallerMapper = new PekkoHttpMarshallerMapper(token);
        return marshaller.map(pekkoHttpMarshallerMapper);
    }

}
