/*
 * -\-\-
 * Spotify Apollo Extra
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
package com.spotify.apollo.logging.extra;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestRunnableFactory;
import com.spotify.apollo.route.RuleMatch;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import okio.ByteString;

import static okio.ByteString.encodeUtf8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RequestLoggingDecoratorTest {

  private static final String MAGIC_TIMESTAMP = "TIMESTAMP";
  private static final BiConsumer<OngoingRequest, RuleMatch<Endpoint>>
      EMPTY_CONTINUATION = (ongoingRequest1, endpointRuleMatch) -> { };
  private RequestLoggingDecorator decorator;

  private RequestRunnableFactory delegateFactory;
  private final Request request = Request.forUri("http://tessting");
  private OngoingRequest ongoingRequest = new FakeRequest(request);

  private TestLogger testLogger = TestLoggerFactory.getTestLogger(RequestLoggingDecorator.class);

  @Rule
  public TestLoggerFactoryResetRule resetRule = new TestLoggerFactoryResetRule();

  @Before
  public void setUp() throws Exception {
    decorator = new RequestLoggingDecorator();

    delegateFactory = ongoingRequest -> matchContinuation -> ongoingRequest.reply(Response.ok());
  }

  @Test
  public void shouldLogRequestAndResponseByDefault() throws Exception {
    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events,
               is(singleEventMatching("- - - {} \"{}\" {} {} \"{}\" \"{}\"",
                                      MAGIC_TIMESTAMP, "GET http://tessting", "200", "-", "-", "-")));
  }

  @Test
  public void shouldLogUserAgentIfPresent() throws Exception {
    ongoingRequest = new FakeRequest(Request.forUri("http://hi").withHeader("User-Agent", "007"));

    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events.size(), is(1));
    assertThat(events.get(0).getArguments().get(5), is("007"));
  }

  @Test
  public void shouldLogRefererIfPresent() throws Exception {
    ongoingRequest = new FakeRequest(Request.forUri("http://hi").withHeader("Referer", "www.spotify.com"));

    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events.size(), is(1));
    assertThat(events.get(0).getArguments().get(4), is("www.spotify.com"));
  }

  @Test
  public void shouldSendRequestAndResponseToConsumerIfConfigured() throws Exception {
    AtomicReference<Request> reference = new AtomicReference<>();
    decorator.setLogger((request, response) -> reference.set(request.request()));

    decorator.apply(delegateFactory).create(ongoingRequest).run(EMPTY_CONTINUATION);

    assertThat(reference.get(), is(request));
  }

  @Test
  public void shouldLogSizeInBytesIfPresent() throws Exception {
    delegateFactory = ongoingRequest ->
        matchContinuation -> ongoingRequest.reply(Response.forPayload(encodeUtf8("7 bytes")));

    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events,
               is(singleEventMatching("- - - {} \"{}\" {} {} \"{}\" \"{}\"",
                                      MAGIC_TIMESTAMP, "GET http://tessting", "200", "7", "-", "-")));

  }

  @Test
  public void shouldLogDashIfNoReply() throws Exception {
    delegateFactory = ongoingRequest ->
        matchContinuation -> ongoingRequest.drop();

    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events,
               is(singleEventMatching("- - - {} \"{}\" {} {} \"{}\" \"{}\"",
                                      MAGIC_TIMESTAMP, "GET http://tessting", "-", "-", "-", "-")));

  }

  private Matcher<List<LoggingEvent>> singleEventMatching(String message, String... args) {
    return new TypeSafeMatcher<List<LoggingEvent>>() {
      @Override
      protected boolean matchesSafely(List<LoggingEvent> events) {
        return events.size() == 1 &&
               events.get(0).getMessage().equals(message) &&
               argsMatch(events.get(0).getArguments(), Arrays.asList(args));
      }

      private boolean argsMatch(List<Object> actual, List<String> expected) {
        Iterator<String> actualIterator = actual.stream()
            .map(Object::toString)
            .iterator();

        for (String e : expected) {
          if (!actualIterator.hasNext()) {
            return false;
          }

          String a = actualIterator.next();
          if (e.equals(MAGIC_TIMESTAMP)) {
            // not comparing this; it would be slightly safer to validate that it's a timestamp,
            // but that seems to be taking things too far.
            continue;
          } else if (!e.equals(a)) {
            return false;
          }
        }

        return !actualIterator.hasNext();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("A single logging event with message \"" + message + "\" and args " +
                               Arrays.toString(args));
      }
    };
  }

  private List<LoggingEvent> collectLoggingEventsForRequest(OngoingRequest ongoingRequest) {
    RequestRunnableFactory decorated = decorator.apply(delegateFactory);

    decorated.create(ongoingRequest).run(EMPTY_CONTINUATION);

    return testLogger.getLoggingEvents().stream()
        .filter(event -> event.getLevel() == Level.INFO)
        .collect(Collectors.toList());
  }

  private static class FakeRequest implements OngoingRequest {

    private final Request request;

    private FakeRequest(Request request) {
      this.request = request;
    }

    @Override
    public Request request() {
      return request;
    }

    @Override
    public void reply(Response<ByteString> response) {

    }

    @Override
    public void drop() {

    }

    @Override
    public boolean isExpired() {
      return false;
    }
  }
}