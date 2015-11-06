/*
 * Copyright (c) 2013 Spotify AB
 */

package com.spotify.apollo.test.experimental;

import com.google.common.collect.ForwardingDeque;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.google.common.base.Preconditions.checkArgument;

class SynchronizedRingBuffer<T> extends ForwardingDeque<T> {

  private final int capacity;
  private final ArrayDeque<T> deque;

  public volatile int count;

  public SynchronizedRingBuffer(final int capacity) {
    checkArgument((capacity & capacity - 1) == 0, "capacity must be a power of 2");

    this.capacity = capacity;
    deque = new ArrayDeque<>(capacity);
  }

  @Override
  protected Deque<T> delegate() {
    return deque;
  }

  public synchronized T[] copy(T[] into) {
    return deque.toArray(into);
  }

  @Override
  public synchronized boolean add(T element) {
    if (count >= capacity) {
      poll();
    } else {
      count++;
    }
    return super.add(element);
  }

  @Override
  public synchronized boolean offer(T o) {
    if (count >= capacity) {
      poll();
    } else {
      count++;
    }
    return super.offer(o);
  }

  @Override
  public synchronized void addFirst(T t) {
    if (count >= capacity) {
      poll();
    } else {
      count++;
    }
    super.addFirst(t);
  }
}
