package com.nr.agent.instrumentation.mongodb;

/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;

import static com.mongodb.reactivestreams.client.internal.Publishers.publishAndFlatten;
import static java.util.Arrays.asList;

/**
 *  Publisher helper for the Quick Tour.
 *  https://github.com/mongodb/mongo-java-driver/blob/5911dfb1811fb3e6946a6bddf65cdaf0ac774b63/driver-reactive-streams/src/examples/reactivestreams/helpers/PublisherHelpers.java
 */
public final class PublisherHelpers {

    /**
     * Creates a {@code Publisher<ByteBuffer>} from the ByteBuffers
     * @param byteBuffers the bytebuffers
     * @return a {@code Publisher<ByteBuffer>}
     */
    public static Publisher<ByteBuffer> toPublisher(final ByteBuffer... byteBuffers) {
        return publishAndFlatten(callback -> callback.onResult(asList(byteBuffers), null));
    }

    private PublisherHelpers() {
    }
}