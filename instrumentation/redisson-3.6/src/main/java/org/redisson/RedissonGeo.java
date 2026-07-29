package org.redisson;

import java.util.List;
import java.util.Map;

import org.redisson.api.GeoEntry;
import org.redisson.api.GeoOrder;
import org.redisson.api.GeoPosition;
import org.redisson.api.GeoUnit;
import org.redisson.api.RFuture;
import org.redisson.api.RGeo;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonGeo<V> implements RGeo<V> {


    
    public RFuture<Long> addAsync(double longitude, double latitude, V member) {
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

    public RFuture<Long> addAsync(GeoEntry... entries) {
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

    public RFuture<Double> distAsync(V firstMember, V secondMember, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DIST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    @SuppressWarnings("unchecked")
	public RFuture<Map<V, String>> hashAsync(V... members) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("HASH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    @SuppressWarnings("unchecked")
    public RFuture<Map<V, GeoPosition>> posAsync(V... members) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<List<V>> radiusAsync(double longitude, double latitude, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<List<V>> radiusAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<List<V>> radiusAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<List<V>> radiusAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, Double>> radiusWithDistanceAsync(double longitude, double latitude, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Map<V, Double>> radiusWithDistanceAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Map<V, Double>> radiusWithDistanceAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, Double>> radiusWithDistanceAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(double longitude, double latitude, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<List<V>> radiusAsync(V member, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<List<V>> radiusAsync(V member, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<List<V>> radiusAsync(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<List<V>> radiusAsync(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, Double>> radiusWithDistanceAsync(V member, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Map<V, Double>> radiusWithDistanceAsync(V member, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Map<V, Double>> radiusWithDistanceAsync(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public Map<V, Double> radiusWithDistance(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, Double>> radiusWithDistanceAsync(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHDISTANCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(V member, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(V member, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Map<V, GeoPosition>> radiusWithPositionAsync(V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSWITHPOSITION");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

	
	public RFuture<Long> radiusStoreToAsync(String destName, double longitude, double latitude, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSSTORETO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	
	public RFuture<Long> radiusStoreToAsync(String destName, double longitude, double latitude, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSSTORETO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	
	public RFuture<Long> radiusStoreToAsync(String destName, double longitude, double latitude, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSSTORETO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	
	public RFuture<Long> radiusStoreToAsync(String destName, V member, double radius, GeoUnit geoUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSSTORETO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	
	public RFuture<Long> radiusStoreToAsync(String destName, V member, double radius, GeoUnit geoUnit, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSSTORETO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	
	public RFuture<Long> radiusStoreToAsync(String destName, V member, double radius, GeoUnit geoUnit, GeoOrder geoOrder, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RADIUSSTORETO");
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
