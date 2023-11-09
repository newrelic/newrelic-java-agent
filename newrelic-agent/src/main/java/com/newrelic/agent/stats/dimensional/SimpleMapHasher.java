package com.newrelic.agent.stats.dimensional;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

class SimpleMapHasher implements MapHasher {
    private static final Charset CHARSET = Charsets.UTF_8;
    static final MapHasher INSTANCE = new SimpleMapHasher();

    private SimpleMapHasher() {}

    public long hash(Map<String, Object> attributes) {
        if (attributes.isEmpty()) {
            return Long.MAX_VALUE;
        }
        final Hasher hasher = Hashing.murmur3_128().newHasher();
        if (attributes.size() == 1) {
            attributes.forEach((key, value) -> {
                hasher.putString(key, CHARSET);
                putValue(hasher, value);
            });
            return hasher.hash().asLong();
        }
        final List<String> keys = new ArrayList<>(attributes.keySet());
        // deterministically order keys
        keys.sort(Comparator.comparingInt(String::hashCode));
        keys.forEach(key -> {
            hasher.putString(key, CHARSET);
            putValue(hasher, attributes.get(key));
        });
        return hasher.hash().asLong();
    }

    static void putValue(Hasher hasher, Object value) {
        if (value instanceof Boolean) {
            hasher.putBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                hasher.putInt(((Integer)value).intValue());
            } else if (value instanceof Long) {
                hasher.putLong(((Long)value).longValue());
            } else if (value instanceof Float) {
                hasher.putFloat(((Float)value).floatValue());
            } else if (value instanceof Double) {
                hasher.putDouble(((Double) value).doubleValue());
            } else {
                hasher.putString(value.toString(), CHARSET);
            }
        } else {
            hasher.putString(value.toString(), CHARSET);
        }
    }
}
