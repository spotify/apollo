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

    if (method == null || path == null) {
      LOG.warn("Invalid URI sent {} {} by service {}",
               message.method(), message.uri(), message.service());
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

  private String readParameterValue(Router.Result<Rule<T>> result, int i) {
    switch (result.paramType(i)) {
      case SEGMENT:
        return result.paramValueDecoded(i).toString();
      case PATH:
        return result.paramValue(i).toString();
      default:
        LOG.error("Unknown rut parameter type {}, URI-decoding parameter value (raw {}, decoded {})",
                  result.paramType(i), result.paramValue(i), result.paramValueDecoded(i));
        return result.paramValueDecoded(i).toString();
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
               message.method(), message.uri(), message.service(), e);
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
