package com.spotify.apollo.route;

import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Response;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import okio.ByteString;

import static org.junit.Assert.assertEquals;

/**
 * Copyright (C) 2015 Spotify AB.
 */
public class HtmlSerializerMiddlewaresTest {

  public static final String TEST_TEMPLATE_FTL = "test_template.ftl";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testHtmlSerialize() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo", rq -> ImmutableMap.of("match", "yo"))
            .withMiddleware(HtmlSerializerMiddlewares.htmlSerialize(TEST_TEMPLATE_FTL))
            .handler().invoke(null).toCompletableFuture().get();
    checkContentTypeAndBody(response);
  }

  @Test
  public void testHtmlSerializeResponse() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo", rq -> Response.forPayload(ImmutableMap.of("match", "yo")))
            .withMiddleware(HtmlSerializerMiddlewares.htmlSerializeResponse(TEST_TEMPLATE_FTL))
            .handler().invoke(null).toCompletableFuture().get();
    checkContentTypeAndBody(response);
  }

  @Test
  public void testHtmlSerializeSync() throws Exception {
    Response<ByteString> response = HtmlSerializerMiddlewares.htmlSerializeSync(TEST_TEMPLATE_FTL)
        .apply(rq -> ImmutableMap.of("match", "yo"))
        .invoke(null).toCompletableFuture().get();
    checkContentTypeAndBody(response);
  }

  @Test
  public void testHtmlSerializeResponseSync() throws Exception {
    Response<ByteString> response = HtmlSerializerMiddlewares.htmlSerializeResponseSync(TEST_TEMPLATE_FTL)
        .apply(rq -> Response.forPayload(ImmutableMap.of("match", "yo")))
        .invoke(null).toCompletableFuture().get();
    checkContentTypeAndBody(response);
  }

  @Test
  public void testException() throws Exception {
    expectedException.expect(RuntimeException.class);
    ImmutableMap<String, String> map = ImmutableMap.of("nomatch", "this");
    HtmlSerializerMiddlewares.serialize(TEST_TEMPLATE_FTL, map);
  }

  private void checkContentTypeAndBody(final Response<ByteString> response) {
    assertEquals("<html>yo</html>\n", response.payload().get().utf8());
    assertEquals("text/html; charset=UTF8", response.headers().get("Content-Type"));
  }

}
