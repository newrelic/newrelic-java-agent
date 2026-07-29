package org.redisson;

import java.util.Collection;
import java.util.List;

import org.redisson.api.RFuture;
import org.redisson.api.RList;
import org.redisson.api.SortOrder;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonListMultimapValues<V> implements RList<V> {

	
    public RFuture<Boolean> deleteAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DELETE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> sizeAsync() {
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

    public RFuture<List<V>> readAllAsync() {
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

    public boolean add(V e) {
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

    public RFuture<Boolean> addAsync(V e) {
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

    public RFuture<Boolean> removeAsync(Object o, int count) {
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

    public RFuture<Boolean> containsAllAsync(Collection<?> c) {
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

    public boolean addAll(Collection<? extends V> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> addAllAsync(final Collection<? extends V> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> addAllAsync(int index, Collection<? extends V> coll) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public boolean addAll(int index, Collection<? extends V> coll) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> removeAllAsync(Collection<?> c) {
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

    public RFuture<Boolean> retainAllAsync(Collection<?> c) {
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

    public void clear() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CLEAR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    public RFuture<List<V>> getAsync(int ...indexes) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<V> getAsync(int index) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public V get(int index) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public V set(int index, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<V> setAsync(int index, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public void fastSet(int index, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    public RFuture<Void> fastSetAsync(int index, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public void add(int index, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    public V remove(int index) {
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
    

    public RFuture<V> removeAsync(int index) {
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

    public void fastRemove(int index) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTREMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }
    
    public RFuture<Void> fastRemoveAsync(int index) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTREMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Boolean> containsAsync(Object o) {
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

    public RFuture<Integer> lastIndexOfAsync(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("INDEXOF");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public void trim(int fromIndex, int toIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRIM");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    
    public RFuture<Void> trimAsync(int fromIndex, int toIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRIM");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }


    public RList<V> subList(int fromIndex, int toIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SUBLIST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> addAfterAsync(V elementToFind, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDAFTER");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Integer> addBeforeAsync(V elementToFind, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDAFTER");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public int addAfter(V elementToFind, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDAFTER");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public int addBefore(V elementToFind, V element) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDBEFORE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<List<V>> readSortAsync(SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public List<V> readSort(SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<List<V>> readSortAsync(SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public List<V> readSort(SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public List<V> readSort(String byPattern, SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<List<V>> readSortAsync(String byPattern, SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public <T> Collection<T> readSort(String byPattern, List<String> getPatterns, SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public <T> RFuture<Collection<T>> readSortAsync(String byPattern, List<String> getPatterns, SortOrder order, int offset,
            int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public int sortTo(String destName, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Integer> sortToAsync(String destName, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public List<V> readSort(String byPattern, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<List<V>> readSortAsync(String byPattern, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public <T> Collection<T> readSort(String byPattern, List<String> getPatterns, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public <T> RFuture<Collection<T>> readSortAsync(String byPattern, List<String> getPatterns, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READSORT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public int sortTo(String destName, SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public int sortTo(String destName, String byPattern, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> sortToAsync(String destName, SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public int sortTo(String destName, String byPattern, SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> sortToAsync(String destName, String byPattern, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public int sortTo(String destName, String byPattern, List<String> getPatterns, SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> sortToAsync(String destName, String byPattern, SortOrder order, int offset,
            int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public int sortTo(String destName, String byPattern, List<String> getPatterns, SortOrder order, int offset,
            int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> sortToAsync(String destName, String byPattern, List<String> getPatterns,
            SortOrder order) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> sortToAsync(String destName, String byPattern, List<String> getPatterns,
            SortOrder order, int offset, int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SORTTO");
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
