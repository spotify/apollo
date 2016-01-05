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
package com.spotify.apollo.meta.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Size limited gathering of metadata about a single endpoint.
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
