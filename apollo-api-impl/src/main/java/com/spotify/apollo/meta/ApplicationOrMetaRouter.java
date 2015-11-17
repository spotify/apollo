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
package com.spotify.apollo.meta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import com.spotify.apollo.Request;
import com.spotify.apollo.route.ApplicationRouter;
import com.spotify.apollo.route.InvalidUriException;
import com.spotify.apollo.route.RuleMatch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ApplicationOrMetaRouter<E> implements ApplicationRouter<E> {
  final ApplicationRouter<E> applicationRouter;
  final ApplicationRouter<E> metaRouter;

  public ApplicationOrMetaRouter(final ApplicationRouter<E> applicationRouter,
                                 final ApplicationRouter<E> metaRouter) {
    this.applicationRouter = applicationRouter;
    this.metaRouter = metaRouter;
  }

  private static final String META = "/_meta/";

  @Override
  public Optional<RuleMatch<E>> match(Request message) throws InvalidUriException {
    final String uri = message.uri();
    if (isMeta(uri)) {
      return metaRouter.match(message);
    } else {
      return applicationRouter.match(message);
    }
  }

  @VisibleForTesting
  static boolean isMeta(final String uriString) {
    final URI uri;
    try {
      uri = new URI(uriString);
    } catch (URISyntaxException e) {
      return false;
    }
    return uri.getPath().startsWith(META);
  }

  @Override
  public Collection<String> getMethodsForValidRules(Request message) {
    Set<String> union = Sets.newHashSet();
    union.addAll(applicationRouter.getMethodsForValidRules(message));
    union.addAll(metaRouter.getMethodsForValidRules(message));
    return union;
  }

  @Override
  public List<E> getRuleTargets() {
    final ImmutableList.Builder<E> builder = ImmutableList.builder();

    builder.addAll(applicationRouter.getRuleTargets());
    builder.addAll(metaRouter.getRuleTargets());

    return builder.build();
  }
}
