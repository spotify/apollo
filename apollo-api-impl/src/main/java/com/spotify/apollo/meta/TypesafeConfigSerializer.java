/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import java.io.IOException;
import java.util.Map;

/**
 * Custom serializer that unwraps instances of {@link com.typesafe.config.ConfigValue}.
 */
class TypesafeConfigSerializer extends JsonSerializer<ConfigValue> {

  private final boolean withOrigins;

  TypesafeConfigSerializer(boolean withOrigins) {
    this.withOrigins = withOrigins;
  }

  @Override
  public void serialize(ConfigValue value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {


    if (value.valueType() == ConfigValueType.OBJECT) {
      final ConfigObject object = (ConfigObject) value;

      jgen.writeStartObject();
      for (Map.Entry<String, ConfigValue> valueEntry : object.entrySet()) {
        if (withOrigins) {
          final ConfigOrigin origin = valueEntry.getValue().origin();
          jgen.writeStringField(valueEntry.getKey() + "__origin",
                                origin.description()
                                + (origin.filename() != null ? ", " + origin.filename() : ""));
        }
        jgen.writeObjectField(valueEntry.getKey(), valueEntry.getValue());
      }
      jgen.writeEndObject();
    } else if (value.valueType() == ConfigValueType.LIST) {
      final ConfigList list = (ConfigList) value;

      jgen.writeStartArray();
      for (ConfigValue configValue : list) {
        jgen.writeObject(configValue);
      }
      jgen.writeEndArray();
    } else {
      jgen.writeObject(value.unwrapped());
    }
  }
}
