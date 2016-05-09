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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class ListClientDecoratorComparatorTest {

  private ListClientDecoratorComparator comparator;

  private Decorator a;
  private Decorator b;
  private Decorator c;
  private Decorator d;

  @Before
  public void setUp() throws Exception {
    comparator = new ListClientDecoratorComparator(ImmutableList.of(C.class, B.class));

    a = new A();
    b = new B();
    c = new C();
    d = new D();
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

  private static class A extends Decorator { }
  private static class B extends Decorator { }
  private static class C extends Decorator { }
  private static class D extends Decorator { }

  static class Decorator implements ClientDecorator {

    @Override
    public IncomingRequestAwareClient apply(IncomingRequestAwareClient incomingRequestAwareClient) {
      return incomingRequestAwareClient;
    }
  }

}