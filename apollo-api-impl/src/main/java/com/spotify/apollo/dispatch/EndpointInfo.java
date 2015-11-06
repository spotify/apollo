package com.spotify.apollo.dispatch;

import com.spotify.apollo.route.Route.DocString;

import java.util.Optional;

public class EndpointInfo {
  final String uri;
  final String requestMethod;
  final String javaMethodName;
  final Optional<DocString> docString;

  public EndpointInfo(
      String uri,
      String requestMethod,
      String javaMethodName,
      Optional<DocString> docString) {
    this.uri = uri;
    this.requestMethod = requestMethod;
    this.javaMethodName = javaMethodName;
    this.docString = docString;
  }

  public String getUri() {
    return uri;
  }

  public String getRequestMethod() {
    return requestMethod;
  }

  public String getJavaMethodName() {
    return javaMethodName;
  }

  public Optional<DocString> getDocString() {
    return docString;
  }

  public String getName() {
    return requestMethod + ':' + uri;
  }
}
