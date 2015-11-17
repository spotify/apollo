/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test.unit;

import org.junit.Test;

import static com.spotify.apollo.Status.*;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.*;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;

public class StatusTypeMatchersTest {

  @Test
  public void belongsToFamilyMatcherMatchesStatusTypeWithSameFamily() throws Exception {
    assertThat(INTERNAL_SERVER_ERROR, belongsToFamily(SERVICE_UNAVAILABLE.family()));
  }

  @Test
  public void belongsToFamilyMatcherDoesNotMatchStatusTypeWithDifferentFamily() throws Exception {
    assertThat(INTERNAL_SERVER_ERROR, not(belongsToFamily(OK.family())));
  }

  @Test
  public void reasonPhraseMatcherMatchesStatusTypeWithMatchingReasonPhrase() throws Exception {
    assertThat(NO_CONTENT, withReasonPhrase(startsWith("No")));
  }

  @Test
  public void reasonPhraseMatcherDoesNotMatchStatusTypeWithNonMatchingReasonPhrase() throws Exception {
    assertThat(NO_CONTENT, not(withReasonPhrase(startsWith("Service"))));
  }

  @Test
  public void withCodeMatcherMatchesStatusTypeWithSameStatusCode() throws Exception {
    assertThat(OK, withCode(200));
  }

  @Test
  public void withCodeMatcherDoesNotMatchStatusTypeWithDifferentStatusCode() throws Exception {
    assertThat(OK, not(withCode(404)));
  }

  @Test
  public void withTypeCodeMatcherMatchesStatusTypeWithSameStatusCode() throws Exception {
    assertThat(OK, withCode(OK));
  }

  @Test
  public void withTypeCodeMatcherDoesNotMatchStatusTypeWithDifferentStatusCode() throws Exception {
    assertThat(OK, not(withCode(GATEWAY_TIMEOUT)));
  }
}
