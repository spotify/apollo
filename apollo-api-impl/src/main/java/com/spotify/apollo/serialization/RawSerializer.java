/*
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
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
