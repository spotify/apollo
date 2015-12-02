package com.spotify.apollo.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.Environment;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.Route;

import okio.ByteString;

/**
 * This example demonstrates how to setup multiple routes in Apollo.
 *
 * It uses asynchronous routes that calls the Spotify API to fetch some data and then process it.
 */
final class SpotifyApiExample {

  public static void main(String[] args) throws LoadingException {
    HttpService.boot(SpotifyApiExample::init, "spotify-api-example-service", args);
  }

  static void init(Environment environment) {
    ObjectMapper objectMapper = new ObjectMapper();

    AlbumResource albumResource = new AlbumResource(objectMapper);
    ArtistResource artistResource = new ArtistResource(objectMapper);

    environment.routingEngine()
        .registerRoutes(albumResource.routes())
        .registerRoutes(artistResource.routes())
        .registerRoute(Route.sync("GET", "/ping", SpotifyApiExample::ping));
  }

  public static Response<ByteString> ping(RequestContext requestContext) {
    return Response.ok().withPayload(ByteString.encodeUtf8("pong!"));
  }
}
