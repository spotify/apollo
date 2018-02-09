/*
 * -\-\-
 * Spotify Apollo Entity Middleware
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.Exploratory;
import com.spotify.apollo.RequestContext;
import java.io.IOException;
import java.util.Objects;
import okio.ByteString;

/**
 * Codec for writing and reading values using a Jackson {@link ObjectMapper}.
 */
@Exploratory
public class JacksonEntityCodec implements Codec, EntityCodec {

  private static final String APPLICATION_JSON = "application/json";

  private final ObjectMapper objectMapper;

  private JacksonEntityCodec(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  /**
   * @deprecated Use {@link #create}
   */
  @Deprecated
  public static EntityCodec forMapper(ObjectMapper objectMapper) {
    return new JacksonEntityCodec(objectMapper);
  }

  public static Codec create(ObjectMapper objectMapper) {
    return new JacksonEntityCodec(objectMapper);
  }

  @Override
  public String defaultContentType() {
    return APPLICATION_JSON;
  }

  @Override
  public <E> ByteString write(E entity, Class<? extends E> clazz) throws IOException {
    return ByteString.of(objectMapper.writeValueAsBytes(entity));
  }

  @Override
  public <E> E read(ByteString data, Class<? extends E> clazz) throws IOException {
    return objectMapper.readValue(data.toByteArray(), clazz);
  }

  @Override
  public <E> EncodedResponse write(E entity, Class<? extends E> cls, RequestContext ctx)
      throws IOException {
    return EncodedResponse.create(
        ByteString.of(objectMapper.writeValueAsBytes(entity)),
        APPLICATION_JSON);
  }

  @Override
  public <E> E read(ByteString data, Class<? extends E> cls, RequestContext ctx)
      throws IOException {
    return objectMapper.readValue(data.toByteArray(), cls);
  }
}
