/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta.model;

import com.google.common.collect.Maps;

import com.typesafe.config.ConfigObject;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.norberg.automatter.AutoMatter;

/**
 * TODO: document.
 */
public interface Model {

  @AutoMatter
  interface MetaInfo {
    String buildVersion();
    String containerVersion();
    @Nullable String systemVersion();

    double serviceUptime();
  }

  @AutoMatter
  interface LoadedConfig {
    ConfigObject spNode();
  }

  class EndpointsInfo {
    public String docstring;
    public List<EndpointInfo> endpoints;
  }

  class EndpointInfo {
    public String methodName;
    public String uri;
    public List<String> method;
    public String docstring;
    public PayloadSchema requestPayloadSchema = new PayloadSchema();
    public PayloadSchema replyPayloadSchema = new PayloadSchema();
    public List<QueryParameter> queryParameters;
  }

  class ExternalCallsInfo {
    public Map<String, List<EndpointInfo>> incoming = Maps.newTreeMap();
    public Map<String, List<EndpointInfo>> outgoing = Maps.newTreeMap();
  }

  class QueryParameter {
    public final String name;

    public QueryParameter(String name) {
      this.name = name;
    }
  }

  class PayloadSchema {
    public String contentType;
  }
}
