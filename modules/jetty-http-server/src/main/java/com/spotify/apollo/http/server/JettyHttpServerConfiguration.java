package com.spotify.apollo.http.server;

/**
 * TODO: document!
 */
public class JettyHttpServerConfiguration {
  private static final String DEFAULT_HTTP_ADDRESS = "0.0.0.0";
  private static final int DEFAULT_TTL_MILLIS = 30000;
  private static final int DEFAULT_WORKER_THREADS =
      Math.max(Runtime.getRuntime().availableProcessors()/4, 2);
  private static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 300; // SECONDS
  private static final int DEFAULT_MAX_HTTP_CHUNK_LENGTH = 128 * 1024; // 128 kB

  private final String address = DEFAULT_HTTP_ADDRESS;
  private final Integer port = null;
  private final String registrationName = null;
  private final long ttlMillis = DEFAULT_TTL_MILLIS;
  private final int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
  private final int workerThreads = DEFAULT_WORKER_THREADS;
  private final int maxHttpChunkLength = DEFAULT_MAX_HTTP_CHUNK_LENGTH;
  private final boolean useFirstPathSegmentAsAuthority  = false;

  public String address() {
    return address;
  }

  public Integer port() {
    return port;
  }

  public String registrationName() {
    return registrationName;
  }

  public long ttlMillis() {
    return ttlMillis;
  }

  public int keepAliveTimeout() {
    return keepAliveTimeout;
  }

  public int workerThreads() {
    return workerThreads;
  }

  public int maxHttpChunkLength() {
    return maxHttpChunkLength;
  }

  public boolean useFirstPathSegmentAsAuthority() {
    return useFirstPathSegmentAsAuthority;
  }
}
