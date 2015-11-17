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
