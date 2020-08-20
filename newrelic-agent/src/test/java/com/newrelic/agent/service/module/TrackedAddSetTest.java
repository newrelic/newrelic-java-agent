package com.newrelic.agent.service.module;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TrackedAddSetTest {
    @Test
    public void resetAddedOnlyTest() {
        TrackedAddSet<String> target = new TrackedAddSet<>();
        target.accept("a");
        target.accept("b");
        target.accept("c");
        assertThat(target.resetReturningAdded(), Matchers.containsInAnyOrder("a", "b", "c"));
        target.accept("d");
        target.accept("e");
        assertThat(target.resetReturningAdded(), Matchers.containsInAnyOrder("d", "e"));
    }

    @Test
    public void resetAllTest() {
        TrackedAddSet<String> target = new TrackedAddSet<>();
        target.accept("a");
        target.accept("b");
        target.accept("c");
        assertThat(target.resetReturningAll(), Matchers.containsInAnyOrder("a", "b", "c"));
        target.accept("e");
        target.accept("d");
        assertThat(target.resetReturningAll(), Matchers.containsInAnyOrder("a", "b", "c", "d", "e"));
    }

    @Test
    public void duplicateChecks() {
        TrackedAddSet<String> target = new TrackedAddSet<>();
        target.accept("a");
        target.accept("a");
        assertThat(target.resetReturningAll(), Matchers.containsInAnyOrder("a"));

        target.accept("a"); // this is in the full set and should not be reported as a new item.
        target.accept("d");
        target.accept("d");
        assertThat(target.resetReturningAdded(), Matchers.containsInAnyOrder("d"));
    }

    @Test
    public void acceptsOneThousandElementsByDefault() {
        TrackedAddSet<Integer> target = new TrackedAddSet<>();
        for(int i = 0; i < 1000; i++) {
            target.accept(i);
        }

        assertEquals("Expected 1000 elements to be added.", 1000, target.resetReturningAdded().size());

        target.accept(1000);
        assertEquals("Expected no additional deltas to be tracked.", 0, target.resetReturningAdded().size());
    }

    @Test
    public void allowsOverriddenMax() {
        TrackedAddSet<Integer> target = new TrackedAddSet<>(10);
        for(int i = 0; i < 10; i++) {
            target.accept(i);
        }

        assertEquals("Expected 10 elements to be added.", 10, target.resetReturningAdded().size());

        target.accept(10);
        assertEquals("Expected no additional deltas to be tracked.", 0, target.resetReturningAdded().size());
    }
}