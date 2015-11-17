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
package com.spotify.apollo.route;

import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import okio.ByteString;

final class RouteRuleBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(RouteRuleBuilder.class);

  // https://tools.ietf.org/html/rfc3986#section-3.1 Scheme
  // scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
  private static final Pattern STRIP_SCHEME_AUTH = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*://[^/]+");

  private RouteRuleBuilder() {
    // no instantiation
  }

  static Rule<Endpoint> toRule(Route<? extends AsyncHandler<Response<ByteString>>> route) {
    final String uri = route.uri();
    final String requestMethod = route.method();
    final String relativeUri = STRIP_SCHEME_AUTH.matcher(uri).replaceFirst("");

    LOG.debug("Found Route with method: {}, uri: {}", requestMethod, relativeUri);

    final Endpoint endpoint = new RouteEndpoint(route);
    final Rule<Endpoint> rule = Rule.fromUri(relativeUri, requestMethod, endpoint);

    return rule;
  }
}
