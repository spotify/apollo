/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.RequestMetadataImpl;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.AsyncContext;

import io.netty.handler.codec.http.QueryStringDecoder;
import okio.ByteString;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApolloRequestHandlerTest {
  private ApolloRequestHandler requestHandler;

  private FakeRequestHandler delegate;
  private MockHttpServletRequest httpServletRequest;
  private MockHttpServletResponse response;
  private Duration requestTimeout;
  private org.eclipse.jetty.server.Request baseRequest;

  @Mock
  AsyncContext asyncContext;
  @Mock
  javax.servlet.ServletOutputStream outputStream;
  @Mock
  private HttpChannel httpChannel;
  @Mock
  private HttpInput httpInput;
  @Mock
  private RequestOutcomeConsumer logger;


  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    delegate = new FakeRequestHandler();
    response = new MockHttpServletResponse();
    RequestMetadata.HostAndPort serverInfo = RequestMetadataImpl.hostAndPort("localhost", 80);

    requestTimeout = Duration.ofMillis(8275523);
    requestHandler = new ApolloRequestHandler(serverInfo, delegate, requestTimeout, logger);

    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     emptyMap());
    // Request is extremely complex, and there doesn't seem to be a fake implementation that can
    // be used in tests, so resorting to spying on it.
    baseRequest = spy(new org.eclipse.jetty.server.Request(httpChannel, httpInput));
    doReturn(asyncContext).when(baseRequest).startAsync();
    when(asyncContext.getResponse()).thenReturn(response);
  }

  @Test
  public void shouldForwardRequestsToDelegate() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    assertThat(delegate.requests, hasItem(instanceOf(OngoingRequest.class)));
    assertThat(delegate.requests, hasSize(1));
  }

  @Test
  public void shouldExtractCorrectMethod() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).method(),
               is("PUT"));
  }

  @Test
  public void shouldExtractCompleteUri() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).uri(),
               is("http://somehost/a/b?q=abc&b=adf&q=def"));
  }

  @Test
  public void shouldExtractFirstQueryParameter() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).parameter("q"),
               is(Optional.of("abc")));
  }

  @Test
  public void shouldExtractAllParameters() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).parameters().get("q"),
               is(ImmutableList.of("abc", "def")));
  }

  @Test
  public void shouldReturnEmptyForMissingParameter() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).parameter("p"),
               is(Optional.<String>empty()));
  }

  @Test
  public void shouldReturnEmptyPayloadWhenMissing() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).payload(),
               is(Optional.empty()));
  }

  @Test
  public void shouldReturnPayloadWhenPresent() throws Exception {
    httpServletRequest.setContent("hi there".getBytes(StandardCharsets.UTF_8));

    assertThat(requestHandler.asApolloRequest(httpServletRequest).payload(),
               is(Optional.of(ByteString.encodeUtf8("hi there"))));
  }

  @Test
  public void shouldExtractCallingServiceFromHeader() throws Exception {
    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     ImmutableMap.of("X-Calling-Service", "testservice"));

    assertThat(requestHandler.asApolloRequest(httpServletRequest).service(),
               is(Optional.of("testservice")));
  }

  @Test
  public void shouldNotExtractCallingServiceFromEmptyHeader() throws Exception {
    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     ImmutableMap.of("X-Calling-Service", ""));

    assertThat(requestHandler.asApolloRequest(httpServletRequest).service(),
               is(Optional.empty()));
  }

  @Test
  public void shouldHandleMissingCallingServiceHeader() throws Exception {
    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     emptyMap());

    assertThat(requestHandler.asApolloRequest(httpServletRequest).service(),
               is(Optional.empty()));
  }


  // I would prefer to test this in a less implementation-dependent way (validating that a timeout
  // is actually sent, rather than that a particular listener is registered), but the servlet APIs
  // aren't designed that way.
  @Test
  public void shouldRegisterTimeoutListenerWithContext() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    verify(asyncContext).addListener(any(TimeoutListener.class));
  }

  // I would prefer to test this in a less implementation-dependent way (validating that a timeout
  // is actually sent, rather than that a particular timeout value is set), but the servlet APIs
  // aren't designed that way.
  @Test
  public void shouldSetTimeout() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    verify(asyncContext).setTimeout(requestTimeout.toMillis());
  }

  @Test
  public void shouldAddProtocolToOngoingRequest() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    assertThat(requestMetadata().httpVersion(), is("HTTP/1.1"));
  }

  @Test
  public void shouldAddRemoteAddressToOngoingRequest() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    assertThat(requestMetadata().remoteAddress(), is(Optional.of(RequestMetadataImpl.hostAndPort("123.45.67.89", 8734))));
  }

  private HttpRequestMetadata requestMetadata() {
    assertThat(delegate.requests, is(iterableWithSize(1)));
    return (HttpRequestMetadata) delegate.requests.get(0).metadata();
  }

  private MockHttpServletRequest mockRequest(
      String method, String requestURI, Map<String, String> headers) {
    QueryStringDecoder decoder = new QueryStringDecoder(requestURI);

    final MockHttpServletRequest mockHttpServletRequest =
        new MockHttpServletRequest(method, decoder.path());

    mockHttpServletRequest.setParameters(
        decoder.parameters().entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> {
              List<String> value = e.getValue();
              return value.toArray(new String[value.size()]);
            })));
    mockHttpServletRequest.setQueryString(requestURI.replace(decoder.path() + "?", ""));

    headers.forEach(mockHttpServletRequest::addHeader);
    mockHttpServletRequest.setProtocol("HTTP/1.1");
    mockHttpServletRequest.setRemoteHost("123.45.67.89");
    mockHttpServletRequest.setRemoteAddr("123.45.67.89");
    mockHttpServletRequest.setRemotePort(8734);

    return mockHttpServletRequest;
  }

  private static class FakeRequestHandler implements RequestHandler {
    private final LinkedList<OngoingRequest> requests = new LinkedList<>();

    @Override
    public void handle(OngoingRequest ongoingRequest) {
      requests.add(ongoingRequest);
    }

  }
}
