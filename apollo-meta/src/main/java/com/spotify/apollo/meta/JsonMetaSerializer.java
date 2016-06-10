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
package com.spotify.apollo.meta;

import com.google.common.base.Throwables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.spotify.apollo.Request;
import com.spotify.apollo.Payloads;
import com.spotify.apollo.Serializer;

import io.norberg.automatter.jackson.AutoMatterModule;
import okio.ByteString;

/**
 * Custom {@link Serializer} for serializing to json. Handles
 * {@link io.norberg.automatter.AutoMatter} and {@link ConfigValue} types.
 */
public class JsonMetaSerializer implements Serializer {

  private static final ObjectMapper MAPPER;
  private static final ObjectMapper OBJECT_MAPPER;

  static {
    final SimpleModule configModule = new SimpleModule();
    final SimpleModule configModuleWithOrigins = new SimpleModule();

//    configModule.addSerializer(ConfigValue.class, new TypesafeConfigSerializer(false));
//    configModuleWithOrigins.addSerializer(ConfigValue.class, new TypesafeConfigSerializer(true));

    MAPPER = new ObjectMapper()
        .registerModule(new AutoMatterModule())
        .registerModule(configModule);

    OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new AutoMatterModule())
        .registerModule(configModuleWithOrigins);
  }

  @Override
  public Payload serialize(Request message, Object o) {
    final ObjectMapper usedMapper = message.parameter("origins").isPresent()
        ? OBJECT_MAPPER
        : MAPPER;

    try {
      return Payloads.create(
          ByteString.encodeUtf8(usedMapper.writeValueAsString(o)),
          "application/json");
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

}
