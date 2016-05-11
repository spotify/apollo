/*
 * -\-\-
 * Spotify Apollo API Environment
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.environment;

import com.google.common.collect.ImmutableList;

import com.spotify.apollo.environment.ClientDecorator.Id;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class ListBasedComparatorTest {

  private ListBasedComparator comparator;

  private Id a;
  private Id b;
  private Id c;
  private Id d;

  @Before
  public void setUp() throws Exception {
    comparator = new ListBasedComparator(ImmutableList.of(id("C"), id("B")));

    a = id("A");
    b = id("B");
    c = id("C");
    d = id("D");
  }

  private Id id(String id) {
    return Id.of(ListBasedComparatorTest.class, id);
  }

  @Test
  public void shouldConsiderTwoUnknownAsEqual() throws Exception {
    assertThat(comparator.compare(a, d), equalTo(0));
    assertThat(comparator.compare(d, a), equalTo(0));
  }

  @Test
  public void shouldConsiderUnknownLargerThanKnown() throws Exception {
    assertThat(comparator.compare(b, d), lessThan(0));
    assertThat(comparator.compare(d, b), greaterThan(0));
  }

  @Test
  public void shouldConsiderTwoKnownBasedOnListOrder() throws Exception {
    assertThat(comparator.compare(b, c), greaterThan(0));
    assertThat(comparator.compare(c, b), lessThan(0));
  }

  @Test
  public void shouldConsiderTwoEqualAsEqual() throws Exception {
    assertThat(comparator.compare(b, b), equalTo(0));
    assertThat(comparator.compare(c, c), equalTo(0));
  }
}