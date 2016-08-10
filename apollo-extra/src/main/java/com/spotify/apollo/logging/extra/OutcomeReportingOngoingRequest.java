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

import com.spotify.apollo.Response;
import com.spotify.apollo.request.ForwardingOngoingRequest;
import com.spotify.apollo.request.OngoingRequest;

import java.util.Optional;

import okio.ByteString;

import static java.util.Objects.requireNonNull;

/**
 * An {@link OngoingRequest} that reports the outcome of a request to its {@link #consumer}. If the
 * request was dropped, the optional {@link Response} parameter will be empty, otherwise, it will
 * contain the response that was sent.
 * <p/>
 * This class is intended to simplify reporting the outcomes of requests via, for instance, logging.
 */
public class OutcomeReportingOngoingRequest extends ForwardingOngoingRequest {
  private final RequestOutcomeConsumer consumer;

  protected OutcomeReportingOngoingRequest(OngoingRequest delegate,
                                           RequestOutcomeConsumer consumer) {
    super(delegate);
    this.consumer = requireNonNull(consumer);
  }

  @Override
  public void reply(Response<ByteString> response) {
    super.reply(response);
    consumer.accept(this, Optional.of(response));
  }

  @Override
  public void drop() {
    super.drop();
    consumer.accept(this, Optional.empty());
  }
}
