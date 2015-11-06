/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta;

import com.spotify.apollo.Serializer;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middlewares;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;
import com.spotify.apollo.meta.model.MetaGatherer;
import com.spotify.apollo.meta.model.Model;

import java.util.stream.Stream;

/**
 * An implementation of a meta api.
 */
public class MetaApplication implements RouteProvider {

  private static final String BASE_META_0 = "/_meta/0";
  private static final Serializer JSON_META = new JsonMetaSerializer();

  private final MetaGatherer gatherer;

  public MetaApplication(MetaGatherer gatherer) {
    this.gatherer = gatherer;
  }

  @Override
  public Stream<Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        Route.sync("GET", BASE_META_0 + "/info", requestContext -> info())
            .withDocString("Collects short, bounded, pieces of information about the service, "
                           + "such as build version and uptime.", ""),
        Route.sync("GET", BASE_META_0 + "/config", requestContext -> config())
            .withDocString("The current loaded config of the service, possibly filtered.", ""),
        Route.sync("GET", BASE_META_0 + "/endpoints", requestContext -> endpoints())
            .withDocString("Lists the endpoints of the service, with as much metadata "
                           + "as available.", ""),
        Route.sync("GET", BASE_META_0 + "/calls", requestContext -> calls())
            .withDocString("Lists outgoing/incoming services to/from which calls have been "
                           + "made from/to the service.", "")
    )
        .map(route -> route.withMiddleware(Middlewares.serialize(JSON_META)));
  }

  Result<Model.MetaInfo> info() {
    return new Result<>(gatherer.info());
  }

  Result<Model.LoadedConfig> config() {
    return new Result<>(gatherer.loadedConfig());
  }

  Result<Model.EndpointsInfo> endpoints() {
    return new Result<>(gatherer.endpoints());
  }

  Result<Model.ExternalCallsInfo> calls() {
    return new Result<>(gatherer.calls());
  }

  public static class Result<T> {
    public final T result;

    public Result(T result) {
      this.result = result;
    }
  }
}
