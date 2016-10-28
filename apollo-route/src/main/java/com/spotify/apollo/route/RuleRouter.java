/*
 * -\-\-
 * Spotify Apollo Route
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.spotify.apollo.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.norberg.rut.Router;

public class RuleRouter<T> implements ApplicationRouter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RuleRouter.class);

  private final List<Rule<T>> rules;
  private final Router<Rule<T>> router;

  private RuleRouter(final Iterable<Rule<T>> rules) {
    this.rules = ImmutableList.copyOf(rules);
    this.router = router(rules);
  }

  @Override
  public Optional<RuleMatch<T>> match(Request message) throws InvalidUriException {
    final String method = message.method();
    final String path = getPath(message);

    if (method == null) {
      LOG.warn("Invalid request for {} sent without method by service {}",
               message.uri(), message.service().orElse("<unknown>"));
      throw new InvalidUriException();
    }

    if (path == null) {
      // Problem already logged in detail upstream
      throw new InvalidUriException();
    }

    final Router.Result<Rule<T>> result = router.result();
    router.route(method, path, result);

    if (!result.isSuccess()) {
      return Optional.empty();
    }

    final Rule<T> rule = result.target();
    final ImmutableMap.Builder<String, String> pathArgs = ImmutableMap.builder();

    for (int i = 0; i < result.params(); i++) {
      pathArgs.put(result.paramName(i), readParameterValue(result, i));
    }

    return Optional.of(new RuleMatch<T>(rule, pathArgs.build()));
  }

  private String readParameterValue(Router.Result<Rule<T>> result, int i)
      throws InvalidUriException {
    switch (result.paramType(i)) {
      case SEGMENT:
        CharSequence decoded = result.paramValueDecoded(i);

        if (decoded == null) {
          LOG.warn("Unable to decode parameter {} with raw value {}", result.paramName(i), result.paramValue(i));
          throw new InvalidUriException();
        }

        return decoded.toString();
      case PATH:
        return result.paramValue(i).toString();
      default:
        LOG.error("Unknown rut parameter type {}, URI-decoding parameter value (raw {}, decoded {})",
                  result.paramType(i), result.paramValue(i), result.paramValueDecoded(i));
        return result.paramValueDecoded(i) == null ? "null" : result.paramValueDecoded(i).toString();
    }
  }

  @Override
  public Collection<String> getMethodsForValidRules(final Request message) {
    // TODO (dano): Ideally match() should return the result so that we don't have to route twice.

    final String path = getPath(message);
    final Router.Result<Rule<T>> result = router.result();
    router.route(message.method(), path, result);

    if (result.status() == Router.Status.NOT_FOUND) {
      return Collections.emptyList();
    }

    return result.allowedMethods();
  }

  @Override
  public List<T> getRuleTargets() {
    List<T> targets = Lists.newArrayListWithCapacity(rules.size());
    for (Rule<T> rule : rules) {
      targets.add(rule.getTarget());
    }
    return targets;
  }

  private String getPath(final Request message) {
    try {
      final URI uri = new URI(message.uri());
      return uri.getRawPath();
    } catch (URISyntaxException e) {
      LOG.warn("Invalid URI sent {} {} by service {}",
               message.method(), message.uri(), message.service().orElse("<unknown>"), e);
      return null;
    }
  }

  /**
   * Create a router from a list of rules.
   */
  public static <T> RuleRouter<T> of(final Iterable<Rule<T>> rules) {
    return new RuleRouter<>(ImmutableList.copyOf(rules));
  }

  private static <T> Router<Rule<T>> router(final Iterable<Rule<T>> rules) {
    final Router.Builder<Rule<T>> b = Router.builder();
    b.optionalTrailingSlash(true);
    for (final Rule<T> rule : rules) {
      for (final String method : rule.getMethods()) {
        b.route(method, rule.getPath(), rule);
      }
    }

    return b.build();
  }
}
