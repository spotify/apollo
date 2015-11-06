package com.spotify.apollo.test.unit;

import com.spotify.apollo.Response;
import org.hamcrest.Matcher;
import org.junit.Test;

import static com.spotify.apollo.test.unit.ResponseMatchers.*;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withCode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ResponseMatchersTest {
  private static final Response<Void> NO_HEADER = Response.ok();
  private static final Response<Void> FOO_BAR_HEADER = Response.<Void>ok().withHeader("foo", "bar");

  private static final Response<String> NO_PAYLOAD = Response.ok();
  private static final Response<String> FOO_PAYLOAD = Response.forPayload("foo");

  @Test
  public void hasNoHeadersMatcherMatchesResponseWithoutHeader() throws Exception {
    assertThat(NO_HEADER, hasNoHeaders());
  }

  @Test
  public void hasNoHeadersMatcherDoesNotMatchResponseWithSomeHeader() throws Exception {
    assertThat(FOO_BAR_HEADER, not(hasNoHeaders()));
  }

  @Test
  public void hasHeaderMatcherMatchesResponseWithMatchingHeader() throws Exception {
    assertThat(FOO_BAR_HEADER, hasHeader("foo", is("bar")));
  }

  @Test
  public void hasHeaderMatcherMatchesResponseWithMultipleHeadersAndMatchingHeader() throws Exception {
    Response<Void> multiMatchingResponse = FOO_BAR_HEADER.withHeader("foo2", "bar2");

    assertThat(multiMatchingResponse, hasHeader("foo", is("bar")));
  }

  @Test
  public void hasHeaderMatcherDoesNotMatchResponseWithNoHeaders() throws Exception {
    assertThat(NO_HEADER, not(hasHeader("foo", is("bar"))));
  }

  @Test
  public void hasHeaderMatcherDoesNotMatchResponseWithMissingHeader() throws Exception {
    assertThat(FOO_BAR_HEADER, not(hasHeader("bazz", is("bar"))));
  }

  @Test
  public void hasHeaderMatcherDoesNotMatchResponseWithHeaderNotMatchingValue() throws Exception {
    assertThat(FOO_BAR_HEADER, not(hasHeader("foo", is("bazz"))));
  }

  @Test
  public void doesNotHaveHeaderMatcherDoesNotMatchResponseWithMatchingHeader() throws Exception {
    assertThat(FOO_BAR_HEADER, not(doesNotHaveHeader("foo")));
  }

  @Test
  public void doesNotHaveHeaderMatcherMatchesResponseWithNonMatchingHeader() throws Exception {
    assertThat(FOO_BAR_HEADER, doesNotHaveHeader("bazz"));
  }

  @Test
  public void doesNotHaveHeaderMatcherMatchesResponseWithNoHeaders() throws Exception {
    assertThat(NO_HEADER, doesNotHaveHeader("foo"));
  }

  @Test
  public void hasNoPayloadMatcherMatchesResponseWithoutPayload() throws Exception {
    assertThat(NO_PAYLOAD, hasNoPayload());
  }

  @Test
  public void hasNoPayloadMatcherDoesNotMatchesResponseWithPayload() throws Exception {
    assertThat(FOO_PAYLOAD, not(ResponseMatchers.<String>hasNoPayload()));
  }

  @Test
  public void hasPayloadMatcherMatchesResponseWithMatchingPayload() throws Exception {
    assertThat(FOO_PAYLOAD, hasPayload(is("foo")));
  }

  @Test
  public void hasPayloadMatcherDoesNotMatchResponseWithNonMatchingPayload() throws Exception {
    assertThat(FOO_PAYLOAD, not(hasPayload(is("bar"))));
  }

  @Test
  public void hasPayloadMatcherDoesNotMatchResponseWithNoPayload() throws Exception {
    assertThat(NO_PAYLOAD, not(hasPayload(is("foo"))));
  }

  @Test
  public void hasPayloadMatcherDoesCallPayloadMatcherWhenResponseHasNoPayload() throws Exception {
    Matcher<String> payloadMatcher = spy(is("foo"));

    assertThat(NO_PAYLOAD, not(hasPayload(payloadMatcher)));

    verify(payloadMatcher, times(0)).matches(any());
  }

  @Test
  public void hasStatusMatcherMatchesResponseWithMatchingStatusType() throws Exception {
    assertThat(Response.ok(), hasStatus(withCode(200)));
  }

  @Test
  public void hasStatusMatcherDoesNotMatchResponseWithNonMatchingStatusType() throws Exception {
    assertThat(Response.ok(), not(hasStatus(withCode(400))));
  }
}
