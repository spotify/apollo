/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2017 Spotify AB
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
package com.spotify.apollo.http.server;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestMetadataImpl;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import okio.ByteString;

import static okio.ByteString.encodeUtf8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CombinedFormatLoggerTest {

  private static final String MAGIC_TIMESTAMP = "TIMESTAMP";
  private static final String REMOTE_IP = "1.2.3.4";
  private final RequestOutcomeConsumer consumer = CombinedFormatLogger.logger();

  private final Request request = Request.forUri("http://testing");
  private OngoingRequest ongoingRequest = new FakeRequest(request);

  private final TestLogger testLogger = TestLoggerFactory.getTestLogger(CombinedFormatLogger.class);

  @Rule
  public TestLoggerFactoryResetRule resetRule = new TestLoggerFactoryResetRule();

  @Test
  public void shouldLogRequestAndResponseByDefault() throws Exception {
    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events,
        is(singleEventMatching("{} - - {} \"{}\" {} {} \"{}\" \"{}\"",
            REMOTE_IP, MAGIC_TIMESTAMP,
            "GET http://testing", "200", "-", "-", "-")));
  }

  @Test
  public void shouldLogUserAgentIfPresent() throws Exception {
    ongoingRequest = new FakeRequest(Request.forUri("http://hi").withHeader("User-Agent", "007"));

    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events.size(), is(1));
    assertThat(events.get(0).getArguments().get(6), is("007"));
  }

  @Test
  public void shouldLogRefererIfPresent() throws Exception {
    ongoingRequest = new FakeRequest(Request.forUri("http://hi").withHeader("Referer", "www.spotify.com"));

    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest);

    assertThat(events.size(), is(1));
    assertThat(events.get(0).getArguments().get(5), is("www.spotify.com"));
  }

  @Test
  public void shouldLogSizeInBytesIfPresent() throws Exception {
    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest, Response.forPayload(encodeUtf8("7 bytes")));

    assertThat(events,
        is(singleEventMatching("{} - - {} \"{}\" {} {} \"{}\" \"{}\"",
            REMOTE_IP, MAGIC_TIMESTAMP,
            "GET http://testing", "200", "7", "-", "-")));
  }

  @Test
  public void shouldLogDashIfNoReply() throws Exception {
    List<LoggingEvent> events = collectLoggingEventsForRequest(ongoingRequest, null);

    assertThat(events,
        is(singleEventMatching("{} - - {} \"{}\" {} {} \"{}\" \"{}\"",
            REMOTE_IP, MAGIC_TIMESTAMP,
            "GET http://testing", "-", "-", "-", "-")));
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
    return collectLoggingEventsForRequest(ongoingRequest, Response.ok());
  }

  private List<LoggingEvent> collectLoggingEventsForRequest(OngoingRequest ongoingRequest,
                                                            Response<ByteString> response) {
    consumer.accept(ongoingRequest, Optional.ofNullable(response));

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

    @Override
    public RequestMetadata metadata() {
      return new RequestMetadata() {
        @Override
        public Instant arrivalTime() {
          return Instant.now();
        }

        @Override
        public Optional<HostAndPort> localAddress() {
          return null;
        }

        @Override
        public Optional<HostAndPort> remoteAddress() {
          return Optional.of(new RequestMetadataImpl.HostAndPortImpl() {
            @Override
            public String host() {
              return REMOTE_IP;
            }

            @Override
            public int port() {
              return 0;
            }
          });
        }
      };
    }
  }
}
