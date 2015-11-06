/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
* TODO: document.
*/
class CallsInfo implements MetaGatherer.CallsGatherer {
  private int sizeLimit;
  private Model.EndpointsInfo endpointsInfo = new Model.EndpointsInfo();
  private ConcurrentMap<String, EndpointInfoGatherer>
      endpoints = Maps.newConcurrentMap();

  public CallsInfo(int sizeLimit) {
    this.sizeLimit = sizeLimit;
  }

  Model.EndpointsInfo getEndpointsInfo() {
    endpointsInfo.endpoints = Lists.newArrayListWithCapacity(endpoints.size());
    for (EndpointInfoGatherer endpoint : endpoints.values()) {
      endpointsInfo.endpoints.add(endpoint.endpointInfo());
    }
    return endpointsInfo;
  }

  @Override
  public void setDocstring(String docstring) {
    this.endpointsInfo.docstring = docstring;
  }

  @Override
  public MetaGatherer.EndpointGatherer namedEndpointGatherer(String key) {
    if (!endpoints.containsKey(key)) {
      if (endpoints.size() < sizeLimit) {
        endpoints.putIfAbsent(key, new EndpointInfoGatherer(key, sizeLimit));
      } else {
        if (!endpoints.containsKey(DefaultMetaGatherer.OVER_SIZE_KEY)) {
          endpoints.putIfAbsent(
              DefaultMetaGatherer.OVER_SIZE_KEY, new EndpointInfoGatherer(
              DefaultMetaGatherer.OVER_SIZE_KEY, sizeLimit));
        }
        return endpoints.get(DefaultMetaGatherer.OVER_SIZE_KEY);
      }
    }
    return endpoints.get(key);
  }

  @Override
  public MetaGatherer.EndpointGatherer uriMethodsEndpointGatherer(String uri, List<String> methods) {
    StringBuilder key = new StringBuilder(uri);
    key.append('[');
    boolean first = true;
    for (String method : methods) {
      if (first) {
        first = false;
      } else {
        key.append(',');
      }
      key.append(method);
    }
    key.append(']');
    return namedEndpointGatherer(key.toString());
  }

  @Override
  public MetaGatherer.EndpointGatherer uriEndpointGatherer(String uri) {
    return namedEndpointGatherer(uri);
  }
}
