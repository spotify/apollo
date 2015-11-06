/*
 * Copyright (c) 2013-2014 Spotify AB
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
