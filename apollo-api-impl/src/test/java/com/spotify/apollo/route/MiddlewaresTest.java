package com.spotify.apollo.route;

import com.google.common.collect.ImmutableList;

import com.spotify.apollo.Payloads;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Serializer;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import okio.ByteString;

import static com.spotify.apollo.Status.CREATED;
import static com.spotify.apollo.Status.NOT_MODIFIED;
import static com.spotify.apollo.Status.NO_CONTENT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MiddlewaresTest {

  @Mock
  AsyncHandler<Response<ByteString>> delegate;
  @Mock
  AsyncHandler<Object> serializationDelegate;

  @Mock
  RequestContext requestContext;

  @Mock
  Request request;

  @Mock
  Serializer serializer;

  CompletableFuture<Response<ByteString>> future;
  CompletableFuture<Object> serializationFuture;

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    future = new CompletableFuture<>();
    serializationFuture = new CompletableFuture<>();

    when(requestContext.request()).thenReturn(request);
    when(request.method()).thenReturn("GET");

    when(delegate.invoke(requestContext)).thenReturn(future);
    when(serializationDelegate.invoke(requestContext)).thenReturn(serializationFuture);
  }

  @Test
  public void asShouldSerializeCharSequenceAsString() throws Exception {
    serializationFuture.complete(new StringBuilder("hi there"));

    assertThat(getResult(Middlewares.autoSerialize(serializationDelegate)).payload(),
               equalTo(Optional.of(ByteString.encodeUtf8("hi there"))));
  }

  @Test
  public void asShouldSerializeStringAsString() throws Exception {
    serializationFuture.complete("hi there");

    assertThat(getResult(Middlewares.autoSerialize(serializationDelegate)).payload(),
               equalTo(Optional.of(ByteString.encodeUtf8("hi there"))));
  }

  @Test
  public void asShouldSerializeResponseCharSequenceAsString() throws Exception {
    serializationFuture.complete(Response.forPayload("hi there"));

    assertThat(getResult(Middlewares.autoSerialize(serializationDelegate)).payload(),
               equalTo(Optional.of(ByteString.encodeUtf8("hi there"))));
  }

  @Test
  public void asShouldNotSerializeByteString() throws Exception {
    ByteString byteString = ByteString.encodeUtf8("this is binary");
    serializationFuture.complete(byteString);

    assertThat(getResult(Middlewares.autoSerialize(serializationDelegate)).payload(),
               equalTo(Optional.of(byteString)));
  }

  @Test
  public void asShouldNotSerializeResponseByteString() throws Exception {
    ByteString byteString = ByteString.encodeUtf8("this is binary");
    serializationFuture.complete(Response.forPayload(byteString));

    assertThat(getResult(Middlewares.autoSerialize(serializationDelegate)).payload(),
               equalTo(Optional.of(byteString)));
  }

  @Test
  public void asShouldSerializeObjectAsJson() throws Exception {
    class TestData {
      public String theString = "hi";
      public int theInteger = 42;
    }

    serializationFuture.complete(new TestData());

    //noinspection ConstantConditions
    String json =
        getResult(Middlewares.autoSerialize(serializationDelegate)).payload().get().utf8();
    assertThat(json, equalToIgnoringWhiteSpace("{\"theString\":\"hi\",\"theInteger\":42}"));
  }

  @Test
  public void asShouldSerializeResponseObjectAsJson() throws Exception {
    class TestData {
      public String theString = "hi";
      public int theInteger = 42;
    }

    serializationFuture.complete(Response.forPayload(new TestData()));

    //noinspection ConstantConditions
    String json =
        getResult(Middlewares.autoSerialize(serializationDelegate)).payload().get().utf8();
    assertThat(json, equalToIgnoringWhiteSpace("{\"theString\":\"hi\",\"theInteger\":42}"));
  }

  @Test
  public void httpShouldSetContentLengthFor200() throws Exception {
    future.complete(Response.of(Status.OK, ByteString.of((byte) 14, (byte) 19)));

    String header = getResult(Middlewares.httpPayloadSemantics(delegate)).headers().get(
        "Content-Length");
    assertThat(Integer.parseInt(header), equalTo(2));
  }

  @Test
  public void httpShouldAppendPayloadFor200() throws Exception {
    ByteString payload = ByteString.of((byte) 14, (byte) 19);
    future.complete(Response.of(Status.OK, payload));

    assertThat(getResult(Middlewares.httpPayloadSemantics(delegate)).payload(),
               equalTo(Optional.of(payload)));
  }

  @Test
  public void httpShouldNotSetContentLengthOrAppendPayloadForInvalidStatusCodes() throws Exception {
    List<StatusType> invalid = ImmutableList.of(Status.createForCode(100), NO_CONTENT, NOT_MODIFIED);

    for (StatusType status : invalid) {
      CompletableFuture<Response<ByteString>> future = new CompletableFuture<>();
      when(delegate.invoke(requestContext)).thenReturn(future);
      future.complete(Response.of(status, ByteString.of((byte) 14, (byte) 19)));

      Response<ByteString> result = getResult(Middlewares.httpPayloadSemantics(delegate));
      String header = result.headers().get("Content-Length");

      assertThat("no content-length for " + status, header, is(nullValue()));
      assertThat("no payload for " + status, result.payload(), is(Optional.<ByteString>empty()));
    }
  }

  @Test
  public void httpShouldSetContentLengthForHeadAnd200() throws Exception {
    when(request.method()).thenReturn("HEAD");
    future.complete(Response.of(Status.OK, ByteString.of((byte) 14, (byte) 19)));

    String header = getResult(Middlewares.httpPayloadSemantics(delegate)).headers().get(
        "Content-Length");
    assertThat(Integer.parseInt(header), equalTo(2));
  }

  @Test
  public void httpShouldNotAppendPayloadForHeadAnd200() throws Exception {
    when(request.method()).thenReturn("HEAD");
    future.complete(Response.of(Status.OK, ByteString.of((byte) 14, (byte) 19)));

    assertThat(getResult(Middlewares.httpPayloadSemantics(delegate)).payload(),
               is(Optional.<ByteString>empty()));
  }

  @Test
  public void contentTypeShouldAddToNonResponse() throws Exception {
    serializationFuture.complete("hi");

    String contentType = getResult(Middlewares.replyContentType("text/plain")
                                       .apply(serializationDelegate()))
        .headers()
        .get("Content-Type");

    assertThat(contentType, equalTo("text/plain"));
  }

  @Test
  public void contentTypeShouldAddToResponse() throws Exception {
    serializationFuture.complete(Response.forPayload("hi"));

    String contentType =
        getResult(Middlewares.replyContentType("text/plain").apply(serializationDelegate()))
            .headers()
            .get("Content-Type");

    assertThat(contentType, equalTo("text/plain"));
  }

  @Test
  public void serializerShouldSerialize() throws Exception {
    Object response = new Object();
    ByteString serializedPayload = ByteString.encodeUtf8("hi there");
    when(serializer.serialize(any(Request.class), eq(response)))
        .thenReturn(Payloads.create(serializedPayload));

    serializationFuture.complete(response);

    assertThat(getResult(Middlewares.serialize(serializer).apply(serializationDelegate)).payload(),
               equalTo(Optional.of(serializedPayload)));
  }

  @Test
  public void serializerShouldSerializeResponses() throws Exception {
    Object response = new Object();
    ByteString serializedPayload = ByteString.encodeUtf8("hi there");
    when(serializer.serialize(any(Request.class), eq(response)))
        .thenReturn(Payloads.create(serializedPayload));

    serializationFuture.complete(Response.forPayload(response));

    assertThat(getResult(Middlewares.serialize(serializer).apply(serializationDelegate)).payload(),
               equalTo(Optional.of(serializedPayload)));
  }

  @Test
  public void serializerShouldCopyHeadersFromResponses() throws Exception {
    Object response = new Object();
    ByteString serializedPayload = ByteString.encodeUtf8("hi there");
    when(serializer.serialize(any(Request.class), eq(response)))
        .thenReturn(Payloads.create(serializedPayload));

    serializationFuture.complete(Response.forPayload(response).withHeader("X-Foo", "Fie"));

    assertThat(getResult(Middlewares.serialize(serializer).apply(serializationDelegate)).headers()
                   .get("X-Foo"),
               equalTo("Fie"));
  }

  @Test
  public void serializerShouldCopyStatusCodeFromResponses() throws Exception {
    Object response = new Object();
    ByteString serializedPayload = ByteString.encodeUtf8("hi there");
    when(serializer.serialize(any(Request.class), eq(response)))
        .thenReturn(Payloads.create(serializedPayload));

    serializationFuture.complete(Response.of(CREATED, response));

    assertThat(getResult(
        Middlewares.serialize(serializer).apply(serializationDelegate)).statusCode(),
               equalTo(CREATED));
  }

  @Test
  public void serializerShouldSetContentTypeIfPresent() throws Exception {
    Object response = new Object();
    ByteString serializedPayload = ByteString.encodeUtf8("hi there");
    when(serializer.serialize(any(Request.class), eq(response)))
        .thenReturn(Payloads.create(serializedPayload, "coool stuff"));

    serializationFuture.complete(response);

    String contentType = getResult(Middlewares.serialize(serializer).apply(serializationDelegate))
        .headers().get("Content-Type");

    assertThat(contentType, equalTo("coool stuff"));
  }

  @Test
  public void serializerShouldNotSetContentTypeIfAbsent() throws Exception {
    Object response = new Object();
    ByteString serializedPayload = ByteString.encodeUtf8("hi there");
    when(serializer.serialize(any(Request.class), eq(response)))
        .thenReturn(Payloads.create(serializedPayload, Optional.<String>empty()));

    serializationFuture.complete(response);

    String contentType = getResult(Middlewares.serialize(serializer).apply(serializationDelegate))
        .headers().get("Content-Type");

    assertThat(contentType, is(nullValue()));
  }

  @Test
  public void serializerShouldNotSerializeNull() throws Exception {
    serializationFuture.complete(Response.forStatus(Status.BAD_REQUEST));

    Response<ByteString> response =
        getResult(Middlewares.serialize(serializer).apply(serializationDelegate));

    assertThat(response.statusCode(), equalTo(Status.BAD_REQUEST));
    verify(serializer, never()).serialize(any(Request.class), any());
  }

  private <T> AsyncHandler<T> serializationDelegate() {
    //noinspection unchecked
    return (AsyncHandler<T>) serializationDelegate;
  }

  private <T> T getResult(AsyncHandler<T> handler) throws InterruptedException, ExecutionException {
    CompletionStage<T> completionStage = handler.invoke(requestContext);
    CompletableFuture<T> completableFuture = completionStage.toCompletableFuture();
    return completableFuture.get();
  }
}
