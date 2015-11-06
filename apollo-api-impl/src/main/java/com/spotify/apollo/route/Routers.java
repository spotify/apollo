/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.route;

import com.spotify.apollo.dispatch.Endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.spotify.apollo.route.Middlewares.apolloDefaults;

public final class Routers {

  private static final Logger LOG = LoggerFactory.getLogger(Routers.class);

  private Routers() {
    // no instantiation
  }

  public static ApplicationRouter<Endpoint> newRouterFromInspecting(Object... objects) {
    return RuleRouter.of(rules(objects));
  }

  private static List<Rule<Endpoint>> rules(Object... objects) {
    final List<Rule<Endpoint>> rules = new ArrayList<>();

    for (Object object : objects) {
      if (object instanceof Route) {
        rules.add(RouteRuleBuilder.toRule((Route) object));
      } else if (object instanceof RouteProvider) {
        ((RouteProvider) object).routes()
            .map(route -> route.withMiddleware(apolloDefaults()))
            .map(RouteRuleBuilder::toRule)
            .forEachOrdered(rules::add);
      } else {
        throw new IllegalArgumentException("Unknown route/rule instance detected " + object);
      }
    }

    return rules;
  }
}
