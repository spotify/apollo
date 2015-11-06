package com.spotify.apollo.http.client;

import com.google.common.base.Joiner;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import okio.ByteString;

class TransformingCallback implements Callback {

  private static final Joiner HEADER_JOINER = Joiner.on(", ");

  private final CompletableFuture<Response<ByteString>> future;

  TransformingCallback(CompletableFuture<Response<ByteString>> future) {
    this.future = future;
  }

  public static TransformingCallback create(
      CompletableFuture<Response<ByteString>> future) {
    return new TransformingCallback(future);
  }

  @Override
  public void onFailure(Request request, IOException e) {
    final String message = MessageFormat.format("Request {0} failed", request);
    final IOException exception = new IOException(message, e);
    future.completeExceptionally(exception);
  }

  @Override
  public void onResponse(com.squareup.okhttp.Response response) throws IOException {
    future.complete(transformResponse(response));
  }

  static Response<ByteString> transformResponse(com.squareup.okhttp.Response response)
      throws IOException {

    final StatusType status =
        transformStatus(response.code(), Optional.ofNullable(response.message()));

    Response<ByteString> apolloResponse =
        Response.forStatus(status);

    for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
      apolloResponse = apolloResponse.withHeader(
          entry.getKey(),
          HEADER_JOINER.join(entry.getValue()));
    }

    final byte[] bytes = response.body().bytes();
    if (bytes.length > 0) {
      apolloResponse = apolloResponse.withPayload(ByteString.of(bytes));
    }

    return apolloResponse;
  }

  static StatusType transformStatus(int code, Optional<String> message) {
    final StatusType statusType = Status.createForCode(code);

    if (message.isPresent() && !message.get().equals(statusType.reasonPhrase())) {
      return statusType.withReasonPhrase(message.get());
    } else {
      return statusType;
    }
  }
}
