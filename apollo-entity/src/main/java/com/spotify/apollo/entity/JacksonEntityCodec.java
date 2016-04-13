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

import java.io.IOException;
import java.util.Objects;

import okio.ByteString;

/**
 * Codec for writing and reading values using a Jackson {@link ObjectMapper}.
 */
public class JacksonEntityCodec implements EntityCodec {

  private static final String DEFAULT_CONTENT_TYPE = "application/json";

  private final ObjectMapper objectMapper;

  private JacksonEntityCodec(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  public static EntityCodec forMapper(ObjectMapper objectMapper) {
    return new JacksonEntityCodec(objectMapper);
  }

  @Override
  public String defaultContentType() {
    return DEFAULT_CONTENT_TYPE;
  }

  @Override
  public <E> ByteString write(E entity, Class<? extends E> clazz) throws IOException {
    return ByteString.of(objectMapper.writeValueAsBytes(entity));
  }

  @Override
  public <E> E read(ByteString data, Class<? extends E> clazz) throws IOException {
    return objectMapper.readValue(data.toByteArray(), clazz);
  }
}
