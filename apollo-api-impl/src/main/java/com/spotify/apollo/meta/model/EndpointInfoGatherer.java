/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
* TODO: document.
*/
class EndpointInfoGatherer implements MetaGatherer.EndpointGatherer {
  private int sizeLimit;
  private Model.EndpointInfo endpointInfo = new Model.EndpointInfo();
  private Set<String> methods = Sets.newCopyOnWriteArraySet();
  private Set<String> queryParameters = Sets.newCopyOnWriteArraySet();

  public EndpointInfoGatherer(String methodName, int sizeLimit) {
    setMethodName(methodName);
    this.sizeLimit = sizeLimit;
  }

  Model.EndpointInfo endpointInfo() {
    endpointInfo.method = Lists.newArrayList(methods);
    endpointInfo.queryParameters = Lists.newArrayListWithCapacity(queryParameters.size());
    for (String name : queryParameters) {
      endpointInfo.queryParameters.add(new Model.QueryParameter(name));
    }
    return endpointInfo;
  }

  @Override
  public void setMethodName(String methodName) {
    endpointInfo.methodName = methodName;
  }

  @Override
  public void setUri(String uri) {
    endpointInfo.uri = uri;
  }

  @Override
  public void setMethods(List<String> methods) {
    this.methods.clear();
    this.methods.addAll(methods);
  }

  @Override
  public void addMethod(String method) {
    if (methods.size() < sizeLimit) {
      methods.add(method);
    } else {
      methods.add("<over size limit>");
    }
  }

  @Override
  public void setRequestContentType(String contentType) {
    endpointInfo.requestPayloadSchema.contentType = contentType;
  }

  @Override
  public void setResponseContentType(String contentType) {
    endpointInfo.replyPayloadSchema.contentType = contentType;
  }

  @Override
  public void setDocstring(String docstring) {
    endpointInfo.docstring = docstring;
  }

  @Override
  public void addQueryParameterName(String name) {
    if (queryParameters.size() < sizeLimit) {
      queryParameters.add(name);
    } else {
      queryParameters.add("<over size limit>");
    }
  }
}
