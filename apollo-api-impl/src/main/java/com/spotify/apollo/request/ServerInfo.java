package com.spotify.apollo.request;

import java.net.InetSocketAddress;

/**
 * A value type containing information about a server that is listening to some
 * {@link InetSocketAddress}.
 */
public interface ServerInfo {

  /**
   * @return An identifier for this server
   */
  String id();

  /**
   * @return The socket address that this server is listening to
   */
  InetSocketAddress socketAddress();
}
