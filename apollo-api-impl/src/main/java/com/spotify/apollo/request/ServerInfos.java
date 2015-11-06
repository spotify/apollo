package com.spotify.apollo.request;

import com.google.auto.value.AutoValue;

import java.net.InetSocketAddress;

/**
 * Companion class for creating {@link ServerInfo} values
 */
public final class ServerInfos {

  private ServerInfos() {
    // no instantiation
  }

  public static ServerInfo create(String id, InetSocketAddress socketAddress) {
    return new AutoValue_ServerInfos_ServerInfoValue(id, socketAddress);
  }

  @AutoValue
  static abstract class ServerInfoValue implements ServerInfo {
  }
}
