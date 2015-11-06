/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta.model;

import java.util.List;

public interface MetaGatherer {

  // READ
  Model.MetaInfo info();
  Model.LoadedConfig loadedConfig();
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
    void setRequestContentType(String contentType);
    void setResponseContentType(String contentType);
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
