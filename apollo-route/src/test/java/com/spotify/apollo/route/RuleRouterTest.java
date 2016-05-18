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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.spotify.apollo.Request;

import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RuleRouterTest {

  private static final int TARGET = 7;

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testMatchSuccess() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/FRED");

    assertThat(rule.getTarget(), is(TARGET));
    assertTrue(match.isPresent());

    RuleMatch<Integer> matcher = match.get();
    String extracted = matcher.extract(0);
    assertThat(extracted, is("FRED"));
  }

  @Test
  public void testMatchMultiple() {
    Rule<Integer> rule = Rule.fromUri("/<bar>/<baz>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/FRED/BARNEY");

    assertTrue(match.isPresent());
    assertThat(rule.getExtractionCount(), is(2));

    RuleMatch<Integer> matcher = match.get();
    String extracted0 = matcher.extract(0);
    String extracted1 = matcher.extract(1);
    assertThat(extracted0, is("FRED"));
    assertThat(extracted1, is("BARNEY"));
  }

  @Test
  public void testGetExtractionCount() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>", "GET", TARGET);
    assertThat(rule.getExtractionCount(), is(1));
  }

  @Test
  public void testNoArgs() {
    Rule<Integer> rule = Rule.fromUri("/", "GET", TARGET);
    assertThat(rule.getExtractionCount(), is(0));
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/");
    assertTrue(match.isPresent());
  }

  @Test
  public void testNoArgsNoMatch() {
    Rule<Integer> rule = Rule.fromUri("/", "GET", TARGET);
    assertThat(rule.getExtractionCount(), is(0));
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/something");
    assertFalse(match.isPresent());
  }

  @Test
  public void testMatchFail() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bary/FRED");
    assertFalse(match.isPresent());
  }

  @Test
  public void testMatchTrailingSlash() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/FRED/");
    assertTrue(match.isPresent());
  }

  @Test
  public void testMatchTrailingSlashInTemplate() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>/", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/FRED");
    assertTrue(match.isPresent());
  }

  @Test
  public void testDecodes() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/FR%20ED/");
    assertTrue(match.isPresent());
    assertThat(match.get().extract(0), is("FR ED"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testArgumentsWithSameNameThrows() {
    Rule.fromUri("/<bar>/<bar>", "GET", TARGET);
  }

  @Test
  public void testHasArgumentsNames() {
    Rule<Integer> rule = Rule.fromUri("/<d>/<c>/<b>/<a>/<quax:path>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/a/b/c/d/and/some/more");
    assertTrue(match.isPresent());
    assertThat(match.get().getRule().getExtractionCount(), is(5));
    assertThat(match.get().parsedPathArguments().get("d"), is("a"));
    assertThat(match.get().parsedPathArguments().get("c"), is("b"));
    assertThat(match.get().parsedPathArguments().get("b"), is("c"));
    assertThat(match.get().parsedPathArguments().get("a"), is("d"));
    assertThat(match.get().parsedPathArguments().get("quax"), is("and/some/more"));
  }

  @Test
  public void testPreservesOrder() {
    Rule<Integer> rule = Rule.fromUri("/<d>/<c>/<b>/<a>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/a/b/c/d");
    assertTrue(match.isPresent());
    assertThat(match.get().getRule().getExtractionCount(), is(4));
    assertThat(match.get().extract(0), is("a"));
    assertThat(match.get().extract(1), is("b"));
    assertThat(match.get().extract(2), is("c"));
    assertThat(match.get().extract(3), is("d"));
  }

  @Test
  public void testMatchPath() {
    Rule<Integer> rule = Rule.fromUri("/bar/<rest:path>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/and/some/longer/path");
    assertTrue(match.isPresent());
    assertThat(match.get().extract(0), is("and/some/longer/path"));
  }

  @Test
  public void testMatchPathDoesNotDecode() {
    Rule<Integer> rule = Rule.fromUri("/bar/<rest:path>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/and/some%20path/decoded");
    assertTrue(match.isPresent());
    assertThat(match.get().extract(0), is("and/some%20path/decoded"));
  }

  @Test
  public void testMatchPathPlain() {
    Rule<Integer> rule = Rule.fromUri("/<rest:path>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/foo/bar/and/some/longer/path");
    assertTrue(match.isPresent());
    assertThat(match.get().extract(0), is("foo/bar/and/some/longer/path"));
  }

  @Test
  public void testDoesNotMatchPartialPath() {
    Rule<Integer> rule = Rule.fromUri("/some/path/<rest:path>", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/some");
    assertFalse(match.isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPathSegmentMustBeLast() {
    Rule.fromUri("/bar/<rest:path>/more", "GET", TARGET);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPathSegmentCanNotHaveSuffix() {
    Rule.fromUri("/bar/<rest:path>err", "GET", TARGET);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPathSegmentCanNotHavePrefixOrSuffix() {
    Rule.fromUri("/bar/err<rest:path>err", "GET", TARGET);
  }

  @Test
  public void testMatchesEncoded() {
    Rule<Integer> rule = Rule.fromUri("/<bar>/baz", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/FR%2FED/baz");
    assertTrue(match.isPresent());

    final String extract = match.get().extract(0);
    assertThat(extract, is("FR/ED"));
  }

  @Test
  public void testMatchesEncodedUnicode() {
    Rule<Integer> rule = Rule.fromUri("/<bar>/baz", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/FR%2FE%e2%98%83D/baz");
    assertTrue(match.isPresent());

    final String extract = match.get().extract(0);
    assertThat(extract, is("FR/E☃D"));
  }

  @Test
  public void testMatchPathWithPartialSegmentPatternSuffix() {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>.json", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/FRED.json");
    assertTrue(match.isPresent());

    final String extract = match.get().extract(0);
    assertThat(extract, is("FRED"));
  }

  @Test
  public void testMatchPathWithPartialSegmentPatternPrefix() {
    Rule<Integer> rule = Rule.fromUri("/bar/al<baz>", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/alFRED");
    assertTrue(match.isPresent());

    final String extract = match.get().extract(0);
    assertThat(extract, is("FRED"));
  }

  @Test
  public void testMatchPathWithPartialSegmentPatternOnBothSides() {
    Rule<Integer> rule = Rule.fromUri("/bar/al<baz>.json", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/alFRED.json");
    assertTrue(match.isPresent());

    final String extract = match.get().extract(0);
    assertThat(extract, is("FRED"));
  }

  @Test
  public void testMatchPathWithPartialSegmentPatternFaild() {
    Rule<Integer> rule = Rule.fromUri("/bar/al<baz>.json", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/aFRED.jso");
    assertFalse(match.isPresent());
  }

  @Test
  public void testMatchPathWithPartialSegmentPatternWithUnicode() {
    Rule<Integer> rule = Rule.fromUri("/bar/al<baz>.json", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/bar/alFR%e2%98%83ED.json");
    assertTrue(match.isPresent());

    final String extract = match.get().extract(0);
    assertThat(extract, is("FR☃ED"));
  }

  @Test
  public void testMatchFailOnWrongMethod() {
    Rule<Integer> rule = Rule.fromUri("/foo", "GET", TARGET);
    Optional<RuleMatch<Integer>> match = route(rule, "POST", "/foo");
    assertFalse(match.isPresent());
  }

  @Test
  public void shouldAllowDoubleSlashesInPath() throws Exception {
    Rule<Integer> rule = Rule.fromUri("/foo/bar/<rest:path>", "GET", TARGET);

    Optional<RuleMatch<Integer>> match = route(rule, "GET", "/foo/bar//path//double//slashes");
    assertTrue(match.isPresent());
    assertThat(match.get().extract(0), is("/path//double//slashes"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyUnicodeInUriThrows() {
    Rule.fromUri("/fo☃o/<bar>/baz", "GET", TARGET);
  }

  @Test
  public void shouldReturnEmptyCollectionOnNonRoute() throws Exception {
    Rule<Integer> rule = Rule.fromUri("/foo/bar", "GET", TARGET);
    final RuleRouter<Integer> router = RuleRouter.of(ImmutableList.of(rule));

    final Request message = Request.forUri("/foo/notbar", "GET");
    final Collection<String> methodsForValidRules = router.getMethodsForValidRules(message);

    assertTrue(methodsForValidRules.isEmpty());
  }

  @Test
  public void shouldReturnMethodsOnValidRoute() throws Exception {
    Rule<Integer> rule = Rule.fromUri("/foo/bar", "GET", TARGET);
    final RuleRouter<Integer> router = RuleRouter.of(ImmutableList.of(rule));

    final Request message = Request.forUri("/foo/bar", "POST");
    final Collection<String> methodsForValidRules = router.getMethodsForValidRules(message);

    assertThat(methodsForValidRules, hasItem("GET"));
  }

  @Test
  public void shouldReturnAllConfiguredTargets() throws Exception {
    List<Rule<String>> rules = ImmutableList.of(Rule.fromUri("/foo/bar", "POST", "hi"),
                                                Rule.fromUri("/foo/bar", "GET", "ho"));
    final RuleRouter<String> router = RuleRouter.of(rules);

    assertThat(router.getRuleTargets(), equalTo(Lists.transform(rules, rule -> rule.getTarget())));
  }

  @Test
  public void shouldThrowInvalidUriExceptionForBadParameterEncoding() throws Exception {
    Rule<Integer> rule = Rule.fromUri("/bar/<baz>", "GET", TARGET);
    final RuleRouter<Integer> router = RuleRouter.of(ImmutableList.of(rule));
    final Request message = Request.forUri("/bar/c%F6", "GET");

    thrown.expect(InvalidUriException.class);
    router.match(message);
  }

  @Test
  public void shouldThrowInvalidUriExceptionForBadPlaylistMosaicUri() throws Exception {
    Rule<Integer> rule = Rule.fromUri("/user/<baz>/playlist/<boo>", "GET", TARGET);
    final RuleRouter<Integer> router = RuleRouter.of(ImmutableList.of(rule));
    final Request message = Request.forUri("/user/%c3%a8hYh%ae%8d%c9%21%d4A%c4%bd8+7/playlist/56S6B5rXio3Q2MQltr7ZJk", "GET");

    thrown.expect(InvalidUriException.class);
    router.match(message);
  }

  /**
   * Build a router with a single rule and route a message with it.
   */
  private static <T> Optional<RuleMatch<T>> route(final Rule<T> rule, final String method,
                                                  final String uri) {
    final RuleRouter<T> router = RuleRouter.of(ImmutableList.of(rule));
    final Request message = Request.forUri(uri, method);
    try {
      return router.match(message);
    } catch (InvalidUriException e) {
      throw Throwables.propagate(e);
    }
  }
}
