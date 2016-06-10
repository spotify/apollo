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

import java.util.List;

/**
 * Internal interface; don't implement this outside of apollo core.
 */
public interface MetaGatherer {

  // READ
  Model.MetaInfo info();
//  Model.LoadedConfig loadedConfig();
  Model.EndpointsInfo endpoints();
  Model.ExternalCallsInfo calls();

  // WRITE
  CallsGatherer getServiceCallsGatherer();
  CallsGatherer getIncomingCallsGatherer(String service);
  CallsGatherer getOutgoingCallsGatherer(String service);

  interface EndpointGatherer {
    void setMethodName(String methodName);
    void setUri(String uri);
    void setMethods(List<String> methods);
    void addMethod(String method);
    void setDocstring(String docstring);
    void addQueryParameterName(String name);
  }

  interface CallsGatherer {
    void setDocstring(String docstring);
    EndpointGatherer uriMethodsEndpointGatherer(String uri, List<String> methods);
    EndpointGatherer uriEndpointGatherer(String uri);
    EndpointGatherer namedEndpointGatherer(String name);
  }
}
