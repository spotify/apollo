package com.spotify.apollo.http.client;

import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

class HttpClient implements IncomingRequestAwareClient {

  private static final MediaType DEFAULT_CONTENT_TYPE = MediaType.parse("application/octet-stream");

  private final OkHttpClient client;

  HttpClient(OkHttpClient client) {
    this.client = client;
  }

  public static HttpClient create() {
    return new HttpClient(new OkHttpClient());
  }

  @Override
  public CompletionStage<com.spotify.apollo.Response<ByteString>> send(
      com.spotify.apollo.Request apolloRequest,
      Optional<com.spotify.apollo.Request> apolloIncomingRequest) {

    final Optional<RequestBody> requestBody = apolloRequest.payload().map(payload -> {
      final MediaType contentType = apolloRequest.header("Content-Type")
          .map(MediaType::parse)
          .orElse(DEFAULT_CONTENT_TYPE);
      return RequestBody.create(contentType, payload);
    });

    final Request request = new Request.Builder()
        .method(apolloRequest.method(), requestBody.orElse(null))
        .url(apolloRequest.uri())
        .build();

    final CompletableFuture<com.spotify.apollo.Response<ByteString>> result =
        new CompletableFuture<>();

    client.newCall(request).enqueue(TransformingCallback.create(result));

    return result;
  }
}
