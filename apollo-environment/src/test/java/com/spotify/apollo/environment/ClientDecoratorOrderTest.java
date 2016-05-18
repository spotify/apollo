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

import com.spotify.apollo.environment.ClientDecorator.Id;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class ClientDecoratorOrderTest {

  private ClientDecoratorOrder comparator;

  private Id a;
  private Id b;
  private Id c;
  private Id d;
  private Id e;
  private Id f;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    comparator = ClientDecoratorOrder.beginWith(id("C"), id("B")).endWith(id("F"), id("E"));

    a = id("A");
    b = id("B");
    c = id("C");
    d = id("D");
    e = id("E");
    f = id("F");
  }

  private Id id(String id) {
    return Id.of(ClientDecoratorOrderTest.class, id);
  }

  @Test
  public void shouldConsiderTwoBeginningBasedOnListOrder() throws Exception {
    assertThat(comparator.compare(b, c), greaterThan(0));
    assertThat(comparator.compare(c, b), lessThan(0));
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
  public void shouldConsiderTwoEqualAsEqual() throws Exception {
    assertThat(comparator.compare(b, b), equalTo(0));
    assertThat(comparator.compare(c, c), equalTo(0));
  }

  @Test
  public void shouldConsiderEndingAsAfterBeginning() throws Exception {
    assertThat(comparator.compare(b, f), lessThan(0));
    assertThat(comparator.compare(f, b), greaterThan(0));
  }

  @Test
  public void shouldConsiderEndingAsAfterUnknown() throws Exception {
    assertThat(comparator.compare(a, f), lessThan(0));
    assertThat(comparator.compare(f, a), greaterThan(0));
  }

  @Test
  public void shouldConsiderTwoEndingBasedOnOrder() throws Exception {
    assertThat(comparator.compare(e, f), greaterThan(0));
    assertThat(comparator.compare(f, e), lessThan(0));
  }

  @Test
  public void shouldValidateThatAnIdIsntDuplicated() throws Exception {
    comparator = ClientDecoratorOrder.beginWith(Id.of(getClass(), "floopity"));

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("floopity");

    comparator.endWith(Id.of(getClass(), "floopity"));
  }
}