/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.route;

import com.spotify.apollo.Request;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRouter<T> {

  /**
   * Match a request to a configured entity.
   *
   * Throws InvalidUriException if the URI of the request
   * is badly formated. Returns absence if there is no match.
   *
   * @param message The message to find the entity for
   */
  Optional<RuleMatch<T>> match(Request message) throws InvalidUriException;

  /**
   * Returns a collection of valid methods (such as GET and POST) for the URI of the provided
   * request. Will return an empty collection if nothing match the URI.
   *
   * @param message The message to find the methods for
   */
  Collection<String> getMethodsForValidRules(Request message);

  /**
   * Return a list of all target objects that this router handles.
   *
   * @return A list of all targets
   */
  List<T> getRuleTargets();
}
