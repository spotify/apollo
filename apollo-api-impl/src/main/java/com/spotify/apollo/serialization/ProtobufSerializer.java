/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.serialization;

import com.google.protobuf.MessageLite;

import com.spotify.apollo.Request;
import com.spotify.apollo.Payloads;
import com.spotify.apollo.Serializer;
import com.spotify.apollo.route.Middleware;

import java.util.Objects;

import okio.ByteString;

/**
 * @deprecated the new preferred way to do response serialization is via a {@link Middleware},
 * see {@link Serializer}.
 */
@Deprecated
public class ProtobufSerializer implements Serializer {

  @Override
  public Payload serialize(Request request, Object t) {
    return Payloads.create(serialize(t));
  }

  public static ByteString serialize(Object protoMessage) {
    Objects.requireNonNull(protoMessage, "protoMessage");

    return ByteString.of(((MessageLite) protoMessage).toByteArray());
  }
}
