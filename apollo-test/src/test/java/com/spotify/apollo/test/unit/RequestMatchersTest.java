package com.spotify.apollo.test.unit;

import com.spotify.apollo.Request;

import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class RequestMatchersTest {

  @Test
  public void shouldMatchUri() throws Exception {
    Matcher<Request> requestMatcher = RequestMatchers.uri(startsWith("http://hi"));
    assertThat(requestMatcher.matches(Request.forUri("http://hi")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hithere")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hi/there")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hey")), is(false));
  }

  @Test
  public void shouldMatchMethod() throws Exception {
    Matcher<Request> requestMatcher = RequestMatchers.method("POST");

    assertThat(requestMatcher.matches(Request.forUri("http://hi")), is(false));
    assertThat(requestMatcher.matches(Request.forUri("http://hi", "POST")), is(true));
  }
}
