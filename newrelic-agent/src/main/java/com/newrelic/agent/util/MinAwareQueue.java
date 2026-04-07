package com.newrelic.agent.util;

import com.newrelic.agent.model.PriorityAware;

import java.util.Queue;

/***
 * Simple interface extending Queue with a peekLast method to get the tail (minimum priority) element of the queue.
 *
 * The standard Queue interface only gives access to the head element, which for us is the element of maximum priority.
 * Read access to the minimum priority element can be used to avoid performing expensive object allocations
 * for low-priority events that would get rejected by the queue anyway.
 *
 * @param <E>
 */
public interface MinAwareQueue<E extends PriorityAware> extends Queue<E> {
    E peekLast();
}
