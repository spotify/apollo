/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.serialization;

import com.google.common.base.Throwables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.Request;
import com.spotify.apollo.Payloads;
import com.spotify.apollo.Serializer;
import com.spotify.apollo.route.Middleware;
import com.spotify.proto2json.HexBytesFormatter;
import com.spotify.proto2json.JsonFormat;

import okio.ByteString;

/**
 * @deprecated the new preferred way to do response serialization is via a {@link Middleware},
 * see {@link Serializer}.
 */
@Deprecated
public class JsonSerializer implements Serializer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final JsonFormat jsonFormat = new JsonFormat(HexBytesFormatter.INSTANCE);

  @Override
  public Payload serialize(Request request, Object t) {
    return Payloads.create(serialize(t));
  }

  public static ByteString serialize(Object o) {
    if (o instanceof com.google.protobuf.Message) {
      final String json = jsonFormat.printToString((com.google.protobuf.Message) o);
      return ByteString.encodeUtf8(json);
    }

    try {
      return ByteString.of(OBJECT_MAPPER.writeValueAsBytes(o));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }
}
