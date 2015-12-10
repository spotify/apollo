/*
 * -\-\-
 * Spotify Apollo okhttp Client Module
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
package com.spotify.apollo.http.client;

import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

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

    //https://github.com/square/okhttp/wiki/Recipes#per-call-configuration
    OkHttpClient finalClient = client;
    if (apolloRequest.ttl().isPresent()
        && client.getReadTimeout() != apolloRequest.ttl().get().toMillis()) {
      finalClient = client.clone();
      finalClient.setReadTimeout(apolloRequest.ttl().get().toMillis(), TimeUnit.MILLISECONDS);
    }

    finalClient.newCall(request).enqueue(TransformingCallback.create(result));

    return result;
  }
}
