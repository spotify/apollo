/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;

import java.net.InetSocketAddress;

import okio.ByteString;

/**
 * A request that is being processed.
 */
public interface OngoingRequest extends RequestMetadata {

  InetSocketAddress PORT_ZERO = new InetSocketAddress(0);
  ServerInfo UNKNOWN_SERVER_INFO = ServerInfos.create("unknown", PORT_ZERO);

  /**
   * Returns the {@link Request}.
   */
  Request request();

  /**
   * Returns an identifier for the server where this request originated.
   */
  default ServerInfo serverInfo() {
    return UNKNOWN_SERVER_INFO;
  }

  /**
   * Reply to the request with a {@link Response}.
   *
   * @param response  Response to send as reply
   */
  void reply(Response<ByteString> response);

  /**
   * Drop the request.
   */
  void drop();

  boolean isExpired();
}
