/*
 * -\-\-
 * Spotify Apollo API Interfaces
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
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
package com.spotify.apollo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StatusTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldAllowSettingReasonPhrase() throws Exception {
    assertThat(Status.OK.withReasonPhrase("this is toe-dally OK").reasonPhrase(),
               equalTo("this is toe-dally OK"));
  }

  @Test
  public void shouldReplaceNewlineInReasonPhrase() throws Exception {
    StatusType statusType = Status.ACCEPTED.withReasonPhrase("with\nnewline");
    assertThat(statusType.reasonPhrase(), is("with newline"));
  }

  @Test
  public void shouldReplaceCRInReasonPhrase() throws Exception {
    StatusType statusType = Status.ACCEPTED.withReasonPhrase("with\rcarriage return");
    assertThat(statusType.reasonPhrase(), is("with carriage return"));
  }

  @Test
  public void shouldReplaceControlCharsInReasonPhrase() throws Exception {
    StatusType statusType = Status.ACCEPTED.withReasonPhrase("with control\7");
    assertThat(statusType.reasonPhrase(), is("with control "));
  }

  @Test
  public void shouldHaveEqualFamily() throws Exception {
    assertTrue(Status.OK.equalFamily(Status.OK));
    assertTrue(Status.OK.equalFamily(Status.OK.withReasonPhrase("okidoki")));
    assertTrue(Status.OK.equalFamily(Status.NO_CONTENT));
    assertFalse(Status.OK.equalFamily(Status.NOT_FOUND));
  }

  @Test
  public void shouldHaveEqualCode() throws Exception {
    assertTrue(Status.OK.equalCode(Status.OK));
    assertTrue(Status.OK.equalCode(Status.OK.withReasonPhrase("okidoki")));
    assertFalse(Status.OK.equalCode(Status.NO_CONTENT));
    assertFalse(Status.OK.equalCode(Status.NOT_FOUND));
  }
}