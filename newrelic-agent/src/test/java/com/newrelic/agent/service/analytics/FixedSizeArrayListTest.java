package com.newrelic.agent.service.analytics;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class FixedSizeArrayListTest {
    @Test
    public void add_withSpaceAvailable_addsSuccessfully() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertTrue(fixedSizeArrayList.add(1));
        assertEquals(new Integer(1), fixedSizeArrayList.get(0));
    }

    @Test
    public void add_withoutSpaceAvailable_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(1);
        assertTrue(fixedSizeArrayList.add(1));
        assertFalse(fixedSizeArrayList.add(2));
        assertEquals(new Integer(1), fixedSizeArrayList.get(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void get_withInvalidIndex_throwsException() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.get(11);
    }

    @Test
    public void addAll_successfullyAddsAllFromCollection() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        ArrayList<Integer> itemsToAdd = new ArrayList<>();
        for (int i=0; i < 5; i++) {
            itemsToAdd.add(i);
        }

        assertTrue(fixedSizeArrayList.addAll(itemsToAdd));
        assertEquals(5, fixedSizeArrayList.size());
    }

    @Test
    public void set_updatesTargetIndexAndReturnsOldValue() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        assertEquals(new Integer(1), fixedSizeArrayList.set(0, 2));
        assertEquals(new Integer(2), fixedSizeArrayList.get(0));
    }

    @Test
    public void getNumberOfTries_returnsNumberOfAddCalls() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.add(2);
        fixedSizeArrayList.add(3);
        assertEquals(3, fixedSizeArrayList.getNumberOfTries());
    }

    @Test
    public void size_returnsSizeOfArray() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.add(2);
        fixedSizeArrayList.add(3);
        assertEquals(3, fixedSizeArrayList.size());
    }

    @Test
    public void isEmpty_returnsTrueIfNoItemsAdded() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertTrue(fixedSizeArrayList.isEmpty());
    }

    @Test
    public void isEmpty_returnsFalseIfItemsAdded() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.add(2);
        fixedSizeArrayList.add(3);
        assertFalse(fixedSizeArrayList.isEmpty());
    }

    @Test
    public void contains_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertFalse(fixedSizeArrayList.contains(1));
    }

    @Test
    public void toArrayOfType_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        Integer [] arr = {};
        assertNull(fixedSizeArrayList.toArray(arr));
    }

    @Test
    public void remove_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertFalse(fixedSizeArrayList.remove(new Integer(1)));
    }

    @Test
    public void containsAll_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        ArrayList<Integer> list = new ArrayList<>();
        assertFalse(fixedSizeArrayList.containsAll(list));
    }

    @Test
    public void removeAll_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        ArrayList<Integer> list = new ArrayList<>();
        assertFalse(fixedSizeArrayList.removeAll(list));
    }

    @Test
    public void retainAll_returnsFalse() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        ArrayList<Integer> list = new ArrayList<>();
        assertFalse(fixedSizeArrayList.retainAll(list));
    }

    @Test
    public void clear_isANoOp() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.clear();
        assertEquals(1, fixedSizeArrayList.size());
    }

    @Test
    public void addWithIndex_isANoOp() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.add(1, 2);
        assertEquals(1, fixedSizeArrayList.size());
    }

    @Test
    public void removeWithIndex_isANoOp() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.remove(0);
        assertEquals(1, fixedSizeArrayList.size());
    }

    @Test
    public void indexOf_returnsNegOne() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        assertEquals(-1, fixedSizeArrayList.indexOf(1));
    }

    @Test
    public void lastIndexOf_returnsNegOne() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        assertEquals(-1, fixedSizeArrayList.lastIndexOf(1));
    }

    @Test
    public void listIterator_returnsNull() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertNull(fixedSizeArrayList.listIterator());
    }

    @Test
    public void listIteratorWithIndex_returnsNull() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertNull(fixedSizeArrayList.listIterator(0));
    }

    @Test
    public void subList_returnsNull() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        assertNull(fixedSizeArrayList.subList(0, 1));
    }

    @Test
    public void iterator_returnsValidIterator() {
        FixedSizeArrayList<Integer> fixedSizeArrayList = new FixedSizeArrayList<>(10);
        fixedSizeArrayList.add(1);
        fixedSizeArrayList.add(2);
        fixedSizeArrayList.add(3);

        Iterator<Integer> iterator = fixedSizeArrayList.iterator();
        assertNotNull(iterator);
        Integer val = 0;
        while (iterator.hasNext()) {
            assertEquals(++val, iterator.next());
        }
    }
}