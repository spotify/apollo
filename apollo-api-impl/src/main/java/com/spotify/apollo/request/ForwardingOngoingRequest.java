package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.util.Objects;

import okio.ByteString;

/**
 * A delegating implementation of {@link OngoingRequest} useful for implementing decorators.
 */
public abstract class ForwardingOngoingRequest implements OngoingRequest {

  private final OngoingRequest delegate;

  protected ForwardingOngoingRequest(OngoingRequest delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public Request request() {
    return delegate.request();
  }

  @Override
  public void reply(Response<ByteString> response) {
    delegate.reply(response);
  }

  @Override
  public void drop() {
    delegate.drop();
  }

  @Override
  public boolean isExpired() {
    return delegate.isExpired();
  }

  @Override
  public ServerInfo serverInfo() {
    return delegate.serverInfo();
  }
}
