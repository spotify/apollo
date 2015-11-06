package com.spotify.apollo.route;

import com.google.auto.value.AutoValue;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Abstract class to get an Auto Value implementation of the {@link Route} interface
 */
@AutoValue
abstract class RouteImpl<H> implements Route<H> {

  @Override
  public <K> Route<K> copy(String method, String uri, K handler, @Nullable DocString docString) {
    return new AutoValue_RouteImpl<>(method, uri, handler, Optional.ofNullable(docString));
  }
}
