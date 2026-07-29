package org.redisson.reactive;

import java.util.Collection;
import java.util.Map;

import org.reactivestreams.Publisher;
import org.redisson.api.RScoredSortedSet.Aggregate;
import org.redisson.api.RScoredSortedSetReactive;
import org.redisson.client.protocol.ScoredEntry;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonScoredSortedSetReactive<V> implements RScoredSortedSetReactive<V> {

    public Publisher<V> pollFirst() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<V> pollLast() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<V> first() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<V> last() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("LAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> add(final double score, final V object) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> removeRangeByRank(final int startIndex, final int endIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVERANGEBYRANK");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> removeRangeByScore(final double startScore, final boolean startScoreInclusive, final double endScore, final boolean endScoreInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVERANGEBYSCORE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> remove(final V object) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> size() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SIZE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> contains(final V o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Double> getScore(final V o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETSCORE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> rank(final V o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANK");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> containsAll(final Collection<?> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINSALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> removeAll(final Collection<?> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVEALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> retainAll(final Collection<?> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RETAINALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Double> addScore(final V object, final Number value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDSCORE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> valueRange(final int startIndex, final int endIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUERANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<ScoredEntry<V>>> entryRange(final int startIndex, final int endIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ENTRYRANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> valueRange(final double startScore, final boolean startScoreInclusive, final double endScore, final boolean endScoreInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUERANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<ScoredEntry<V>>> entryRange(final double startScore, final boolean startScoreInclusive, final double endScore, final boolean endScoreInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ENTRYRANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> valueRange(final double startScore, final boolean startScoreInclusive, final double endScore, final boolean endScoreInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUERANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<ScoredEntry<V>>> entryRange(final double startScore, final boolean startScoreInclusive, final double endScore, final boolean endScoreInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ENTRYRANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Long> count(final double startScore, final boolean startScoreInclusive, final double endScore,
            final boolean endScoreInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COUNT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> readAll() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> intersection(final String... names) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("INTERSECTION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> intersection(final Aggregate aggregate, final String... names) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("INTERSECTION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> intersection(final Map<String, Double> nameWithWeight) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("INTERSECTION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> intersection(final Aggregate aggregate, final Map<String, Double> nameWithWeight) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("INTERSECTION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> union(final String... names) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("UNION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> union(final Aggregate aggregate, final String... names) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("UNION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> union(final Map<String, Double> nameWithWeight) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("UNION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> union(final Aggregate aggregate, final Map<String, Double> nameWithWeight) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("UNION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> valueRangeReversed(final int startIndex, final int endIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUERANGEREVERSED");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> valueRangeReversed(final double startScore, final boolean startScoreInclusive, final double endScore,
            final boolean endScoreInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUERANGEREVERSED");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<V>> valueRangeReversed(final double startScore, final boolean startScoreInclusive, final double endScore,
            final boolean endScoreInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUERANGEREVERSED");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<ScoredEntry<V>>> entryRangeReversed(final int startIndex, final int endIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ENTRYRANGEREVERSED");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<ScoredEntry<V>>> entryRangeReversed(final double startScore, final boolean startScoreInclusive,
            final double endScore, final boolean endScoreInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ENTRYRANGEREVERSED");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<ScoredEntry<V>>> entryRangeReversed(final double startScore, final boolean startScoreInclusive,
            final double endScore, final boolean endScoreInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ENTRYRANGEREVERSED");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
}
