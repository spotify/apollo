/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.request;

import com.google.auto.value.AutoValue;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;

import java.util.Map;

@AutoValue
public abstract class RequestContexts implements RequestContext {

  public static RequestContext create(
      Request request, Client client, Map<String, String> pathArgs) {
    return new AutoValue_RequestContexts(request, client, pathArgs);
  }
}
