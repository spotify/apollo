/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.route;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import io.norberg.rut.Route;

import static java.util.Collections.singletonList;

/**
 * Simple holder for a rule.
 *
 * @param <T> target type of the rule
 */
public class Rule<T> {

  private final List<String> methods;
  private final String path;
  private final T target;
  private final int extractionCount;

  /**
   * Create a new rule
   *
   * @param uri    uri pattern used for matching for this rule
   * @param method a method to match (GET/PUT etc)
   */
  private Rule(final String uri, final String method, T target) {
    this(uri, singletonList(method), target);
  }

  /**
   * Create a new rule.
   *
   * @param uri     uri pattern used for matching for this rule
   * @param methods a list of methods to match (GET/PUT et al)
   */
  private Rule(final String uri, final List<String> methods, final T target) {
    this.path = uri;
    this.target = target;
    this.methods = processMethods(methods);
    final Route route = Route.of("HEAD", uri);
    final Set<String> duplicateNames = duplicates(route.captureNames());
    if (!duplicateNames.isEmpty()) {
      throw new IllegalArgumentException(
          "duplicate extraction names: " + Joiner.on(',').join(duplicateNames));
    }
    this.extractionCount = route.captureNames().size();
  }

  public List<String> getMethods() {
    return methods;
  }

  public String getPath() {
    return path;
  }

  public T getTarget() {
    return target;
  }

  private static ImmutableList<String> processMethods(List<String> methods) {
    ImmutableList.Builder<String> builder = ImmutableList.<String>builder()
        .addAll(methods);
    if (methods.contains("GET")) {
      builder.add("HEAD");
    }
    return builder.build();
  }

  /**
   * Create a new rule.
   */
  public static <T> Rule<T> fromUri(final String uri, final List<String> methods, final T target) {
    return new Rule<>(uri, methods, target);
  }

  /**
   * Create a new rule.
   */
  public static <T> Rule<T> fromUri(final String uri, final String methods, final T target) {
    return new Rule<>(uri, singletonList(methods), target);
  }

  public int getExtractionCount() {
    return extractionCount;
  }

  private static Set<String> duplicates(final List<String> strings) {
    final Set<String> duplicates = Sets.newHashSet();
    final Set<String> unique = Sets.newHashSet();
    for (final String string : strings) {
      if (unique.contains(string)) {
        duplicates.add(string);
      }
      unique.add(string);
    }
    return duplicates;
  }
}
