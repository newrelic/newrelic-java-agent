package com.mongodb.async.client;

import com.newrelic.api.agent.weaver.SkipIfPresent;


// This instrumentation will cause Segment timeouts if it applies to the async driver
@SkipIfPresent
class MongoDatabaseImpl {
}
