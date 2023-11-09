package com.newrelic.agent.stats.dimensional;

import java.util.Map;

interface MapHasher {
    long hash(Map<String, Object> map);
}
