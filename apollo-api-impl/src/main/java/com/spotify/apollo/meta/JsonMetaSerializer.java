/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta;

import com.google.common.base.Throwables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.spotify.apollo.Request;
import com.spotify.apollo.Payloads;
import com.spotify.apollo.Serializer;
import com.typesafe.config.ConfigValue;

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

    configModule.addSerializer(ConfigValue.class, new TypesafeConfigSerializer(false));
    configModuleWithOrigins.addSerializer(ConfigValue.class, new TypesafeConfigSerializer(true));

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
