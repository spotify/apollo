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

import com.google.common.base.Joiner;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
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
  public void onFailure(Call call, IOException e) {
    final String message = MessageFormat.format("Request {0} failed", call.request());
    final IOException exception = new IOException(message, e);
    future.completeExceptionally(exception);
  }

  @Override
  public void onResponse(Call call, okhttp3.Response response) throws IOException {
    future.complete(transformResponse(response));
  }

  static Response<ByteString> transformResponse(okhttp3.Response response)
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
