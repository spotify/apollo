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
package com.spotify.apollo.http.server;

import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;

import java.util.Optional;
import java.util.function.BiConsumer;

import okio.ByteString;

/**
 * Defines an API for notification of the outcome of a particular request. Once the request has
 * finished processing, this consumer can be called with the originating {@link OngoingRequest} and
 * an optional {@link Response} as parameters. If the {@link Response} {@link Optional} is empty,
 * that means the request was dropped and no response was sent to the caller.
 */
public interface RequestOutcomeConsumer
    extends BiConsumer<OngoingRequest, Optional<Response<ByteString>>>  {

}
