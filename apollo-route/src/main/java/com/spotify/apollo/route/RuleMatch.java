/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.route;

import java.util.Map;

/**
 * The result of matching a rule against an input string.
 *
 * @param <T> target type of the rule
 */
public class RuleMatch<T> {

  private final Rule<T> rule;
  private final Map<String, String> pathArguments;
  private final String[] param;

  public RuleMatch(final Rule<T> rule, final Map<String, String> pathArguments) {
    this.rule = rule;
    this.pathArguments = pathArguments;
    this.param = pathArguments.values().toArray(new String[pathArguments.size()]);
  }

  public Rule<T> getRule() {
    return rule;
  }

  public String extract(final int i) {
    return param[i];
  }

  public Map<String, String> parsedPathArguments() {
    return pathArguments;
  }
}
