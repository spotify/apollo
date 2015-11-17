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
