package com.mongodb;

import com.newrelic.api.agent.weaver.SkipIfPresent;

// Prevent this instrumentation module from applying when the mongodb-3.7 module does.
@SkipIfPresent
public final class MongoClientSettings {
}
