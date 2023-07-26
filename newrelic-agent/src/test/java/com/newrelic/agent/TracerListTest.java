package com.newrelic.agent;

import com.newrelic.agent.tracers.Tracer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TracerListTest {
    private Tracer mockTracer;
    private Set<TransactionActivity> txnActivitySet;
    private TransactionActivity mockTxnActivity;
    private Tracer mockTracerForList;

    @Before
    public void setup() {
        mockTracer = mock(Tracer.class);
        mockTracerForList = mock(Tracer.class);
        mockTxnActivity = mock(TransactionActivity.class, Mockito.RETURNS_DEEP_STUBS);

        List<Tracer> tracers = new ArrayList<>();
        tracers.add(mockTracerForList);
        txnActivitySet = new HashSet<>();
        when(mockTxnActivity.getTracers()).thenReturn(tracers);
        when(mockTxnActivity.getRootTracer()).thenReturn(mockTracer);

        txnActivitySet.add(mockTxnActivity);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withNullActivityList_throwsException() {
        TracerList list = new TracerList(mockTracer, null);
    }

    @Test
    public void size_returnsProperValue() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);

        assertEquals(1, list.size());
    }

    @Test
    public void isEmpty_returnsProperValue() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertFalse(list.isEmpty());

        txnActivitySet.clear();
        list = new TracerList(mockTracer, txnActivitySet);
        assertTrue(list.isEmpty());
    }

    @Test
    public void contains_returnsProperValue() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertTrue(list.contains(mockTracerForList));

        assertFalse(list.contains(mock(Tracer.class)));
    }

    @Test
    public void iterator_returnsIterator() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        Iterator<Tracer> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(iterator.next(), mockTracerForList);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void toArray_returnArrayInstance() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        Object[] array = list.toArray();
        assertEquals(1, array.length);

        Tracer[] array2 = new Tracer[] {};
        array2 = list.toArray(array2);
        assertEquals(1, array2.length);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void add_throwsException() {
        new TracerList(mockTracer, txnActivitySet).add(mockTracer);
    }

    @Test
    public void remove_removesTargetItem() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(1, list.size());
        list.remove(mockTracerForList);
        assertEquals(0, list.size());
    }

    @Test
    public void addAll_addsNewItems() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(1, list.size());

        ArrayList<Tracer> newList = new ArrayList<>();
        newList.add(mock(Tracer.class));
        newList.add(mock(Tracer.class));
        list.addAll(newList);
        assertEquals(3, list.size());

        newList = new ArrayList<>();
        newList.add(mock(Tracer.class));
        newList.add(mock(Tracer.class));
        list.addAll(0, newList);
        assertEquals(5, list.size());
    }

    @Test
    public void removeAll_addsNewItems() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(1, list.size());

        ArrayList<Tracer> newList = new ArrayList<>();
        newList.add(mock(Tracer.class));
        newList.add(mock(Tracer.class));
        list.addAll(newList);
        assertEquals(3, list.size());
        list.removeAll(newList);
        assertEquals(1, list.size());
    }

    @Test
    public void retainAll_returnsTargetItems() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(1, list.size());

        ArrayList<Tracer> newList = new ArrayList<>();
        newList.add(mock(Tracer.class));
        newList.add(mock(Tracer.class));
        list.addAll(newList);
        assertEquals(3, list.size());
        list.retainAll(newList);
        assertEquals(2, list.size());
    }

    @Test
    public void clear_removesAllItems() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(1, list.size());

        list.clear();
        assertEquals(0, list.size());
    }

    @Test
    public void get_retrievesTargetItem() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(mockTracerForList, list.get(0));
    }

    @Test
    public void set_assignsItemToTargetIdx() {
        Tracer tracer = mock(Tracer.class);
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(mockTracerForList, list.get(0));

        list.set(0, tracer);
        assertEquals(tracer, list.get(0));
    }

    @Test
    public void add_assignsItemToTargetIdx() {
        Tracer tracer = mock(Tracer.class);
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(mockTracerForList, list.get(0));

        list.add(0, tracer);
        assertEquals(tracer, list.get(0));
        assertEquals(2, list.size());
    }

    @Test
    public void remove_removesItemFromTargetIdx() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(mockTracerForList, list.get(0));

        list.remove(0);
        assertEquals(0, list.size());
    }

    @Test
    public void indexOf_returnsIdxForTargetItem() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        assertEquals(0, list.indexOf(mockTracerForList));
    }

    @Test
    public void lastIndexOf_returnsIdxForLastTargetItemInList() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        list.add(0, mockTracerForList);
        list.add(0, mockTracerForList);
        list.add(0, mockTracerForList);
        assertEquals(3, list.lastIndexOf(mockTracerForList));
    }

    @Test
    public void subList_returnsTargetSublist() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        list.add(0, mockTracerForList);
        list.add(0, mockTracerForList);
        list.add(0, mockTracerForList);
        assertEquals(1, list.subList(3, 4).size());
    }

    @Test
    public void listIterator_returnsListIteratorInstance() {
        TracerList list = new TracerList(mockTracer, txnActivitySet);
        list.add(0, mockTracerForList);
        list.add(0, mockTracerForList);
        list.add(0, mockTracerForList);

        ListIterator<Tracer> listIterator = list.listIterator();
        assertTrue(listIterator.hasNext());
        assertEquals(0, listIterator.nextIndex());
        listIterator.next();
        assertEquals(1, listIterator.nextIndex());

        listIterator = list.listIterator(3);
        assertTrue(listIterator.hasNext());
        assertEquals(3, listIterator.nextIndex());
    }
}
