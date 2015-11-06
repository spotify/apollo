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
