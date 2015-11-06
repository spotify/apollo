/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.net.InetSocketAddress;

import okio.ByteString;

/**
 * A request that is being processed.
 */
public interface OngoingRequest {

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
