/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.serialization;

import com.spotify.apollo.Payloads;
import com.spotify.apollo.Request;
import com.spotify.apollo.Serializer;
import com.spotify.apollo.route.Middleware;

import okio.ByteString;

/**
 * @deprecated the new preferred way to do response serialization is via a {@link Middleware},
 * see {@link Serializer}.
 */
@Deprecated
public class RawSerializer implements Serializer {

  @Override
  public Payload serialize(Request request, Object t) {
    return Payloads.create(serialize(t));
  }

  public static ByteString serialize(Object o) {
    if (o instanceof ByteString) {
      return (ByteString) o;
    }

    if (o instanceof byte[]) {
      return ByteString.of((byte[]) o);
    }

    throw new IllegalArgumentException("Can not handle " + o.getClass());
  }
}
