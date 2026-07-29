package org.redisson;

import java.util.List;
import java.util.Map;

import org.redisson.api.RFuture;
import org.redisson.api.RGeo;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;
import org.redisson.api.geo.*;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonGeo<V> implements RGeo<V> {


	public RFuture<Long> addAsync(double longitude, double latitude, V member) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Long> addAsync(GeoEntry... entries) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Double> distAsync(V firstMember, V secondMember, GeoUnit geoUnit) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("DIST");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	@SuppressWarnings("unchecked")
	public RFuture<Map<V, String>> hashAsync(V... members) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("HASH");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	@SuppressWarnings("unchecked")
	public RFuture<Map<V, GeoPosition>> posAsync(V... members) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("POS");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<List<V>> searchAsync(GeoSearchArgs args) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("SEARCH");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Map<V, Double>> searchWithDistanceAsync(GeoSearchArgs args) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("SEARCHWITHDISTANCE");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Map<V, GeoPosition>> searchWithPositionAsync(GeoSearchArgs args) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("SEARCHWITHDISTANCE");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Long> storeSearchToAsync(String destName, GeoSearchArgs args) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("SEARCHTO");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Long> storeSortedSearchToAsync(String destName, GeoSearchArgs args) {
		if (!Utils.operationIsSet()) {
			Utils.setOperation("SEARCHSORTEDTO");
		}
		if (!Utils.typeSet()) {
			Utils.setType(this);
		}
		if (!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}
}