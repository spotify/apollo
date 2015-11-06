package com.spotify.apollo.route;

import com.google.common.annotations.VisibleForTesting;

import com.spotify.apollo.Response;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import okio.ByteString;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Allows you to expand a stream of VersionedRoutes to a stream of Routes. Expanding a single
 * com.spotify.apollo.route.VersionedRoute to a Stream of Routes means prefixing the base Route's URI with '/vN' for each N
 * that the in the range [startVersion, lastVersion] where the com.spotify.apollo.route.VersionedRoute is valid and returning
 * new Routes with the new URIs.
 *
 * Example:
 *
 * {@code
 *   com.spotify.apollo.route.Versions versions = com.spotify.apollo.route.Versions.from(0).to(2);
 *
 *   com.spotify.apollo.route.VersionedRoute vr = com.spotify.apollo.route.VersionedRoute.of(route).removedIn(2);
 *
 *   environment.routingEngine()
 *      .registerSafeRoutes(versions.expand(Stream.of(vr)))
 *      .registerSafeRoutes(versions.expand(myResource.versionedRoutes()));
 * }
 */
public class Versions {
  private final int startVersion;
  private final int lastVersion;

  public Versions(int startVersion, int lastVersion) {
    this.startVersion = startVersion;
    this.lastVersion = lastVersion;
  }

  public Stream<Route<AsyncHandler<Response<ByteString>>>> expand(
      Stream<VersionedRoute> versionedRouteStream) {

    List<Route<AsyncHandler<Response<ByteString>>>> routes = expandToRoutes(versionedRouteStream);

    sanityCheck(routes);

    return routes.stream();
  }

  private List<Route<AsyncHandler<Response<ByteString>>>> expandToRoutes(
      Stream<VersionedRoute> versionedRouteStream) {
    return versionedRouteStream.flatMap(
          versionedRoute -> {
            int lowerBound = Math.max(startVersion, versionedRoute.validFrom());
            int upperBoundExclusive = versionedRoute.removedIn().orElse(lastVersion + 1);
            Route<AsyncHandler<Response<ByteString>>> route = versionedRoute.route();

            return IntStream.range(lowerBound, upperBoundExclusive).mapToObj(
                i ->
                    route.copy(
                        route.method(),
                        "/v" + String.valueOf(i) + (route.uri().startsWith("/") ? "" : "/") + route
                            .uri(),
                        route.handler(),
                        route.docString().orElse(null)))
                ;

          }
      ).collect(toList());
  }

  @VisibleForTesting
  static String methodUri(Route<?> route) {
    return route.method() + " " + route.uri();
  }

  private void sanityCheck(List<Route<AsyncHandler<Response<ByteString>>>> routes) {
    Map<String, Long> methodUriCounts = routes.stream()
        .collect(Collectors.groupingBy(Versions::methodUri, counting()));

    Set<String> overlappingMethodUris =
        methodUriCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(toSet());

    if (!overlappingMethodUris.isEmpty()) {
      throw new IllegalArgumentException(
          "versioned routes overlap for the following method/uris: " + overlappingMethodUris);
    }
  }

  public static NeedsTo from(int startVersionInclusive) {
    return new NeedsTo(startVersionInclusive);
  }

  public static class NeedsTo {
    private final int startVersion;

    public NeedsTo(int startVersion) {
      this.startVersion = startVersion;
    }

    public Versions to(int lastVersionInclusive) {
      return new Versions(startVersion, lastVersionInclusive);
    }
  }
}
