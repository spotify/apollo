/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.serialization;

import com.spotify.apollo.Payloads;
import com.spotify.apollo.Request;
import com.spotify.apollo.Serializer;
import com.spotify.apollo.route.Middleware;

/**
 * @deprecated the new preferred way to do response serialization is via a {@link Middleware},
 * see {@link Serializer}.
 */
@Deprecated
public class AutoSerializer implements Serializer {

  @Override
  public Payload serialize(Request message, Object o) {
    return Payloads.create(serialize(o));
  }

  public static okio.ByteString serialize(Object o) {
    if (o instanceof okio.ByteString) {
      return (okio.ByteString) o;
    }

    if (o instanceof String || o instanceof CharSequence) {
      return StringSerializer.serialize(o.toString());
    }

    if (o instanceof byte[]) {
      return RawSerializer.serialize(o);
    }

    return JsonSerializer.serialize(o);
  }
}
