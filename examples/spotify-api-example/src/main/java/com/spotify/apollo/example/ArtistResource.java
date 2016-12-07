package com.spotify.apollo.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.example.data.Album;
import com.spotify.apollo.example.data.Artist;
import com.spotify.apollo.example.data.Track;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.JsonSerializerMiddlewares;
import com.spotify.apollo.route.Route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import okio.ByteString;

/**
 * The artist resource demonstrates how to use asynchronous routes in Apollo with query arguments.
 */
public class ArtistResource {

  private static final String SPOTIFY_API = "https://api.spotify.com";
  private static final String SEARCH_API = SPOTIFY_API + "/v1/search";
  private static final String ARTIST_API = SPOTIFY_API + "/v1/artists";

  private final ObjectMapper objectMapper;
  private final ObjectWriter objectWriter;

  public ArtistResource(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.objectWriter = objectMapper.writer();
  }

  public Stream<Route<AsyncHandler<Response<ByteString>>>> routes() {
    // The artist resource has one parameterized route.
    return Stream.of(
        Route.async("GET", "/artists/toptracks/<country>", this::getArtistTopTracks).withDocString(
				"Get top tracks for a specified country.", 
				"Uses the public Spotify API at https://api.spotify.com to get the current top tracks for a specific country.")
          .withMiddleware(JsonSerializerMiddlewares.jsonSerializeResponse(objectWriter))
    );
  }

  CompletionStage<Response<ArrayList<Track>>> getArtistTopTracks(RequestContext requestContext) {
    // Validate request
    Optional<String> query = requestContext.request().parameter("q");
    if (!query.isPresent()) {
      return CompletableFuture.completedFuture(
          Response.forStatus(Status.BAD_REQUEST.withReasonPhrase("No search query")));
    }
    String country = requestContext.pathArgs().get("country");

    // We need to first query the search API, parse the result, then query top-tracks.
    Request searchRequest = Request.forUri(SEARCH_API + "?type=artist&q=" + query.get());
    Client client = requestContext.requestScopedClient();
    return client
        .send(searchRequest)
        .thenComposeAsync(response -> {
          String topArtistId = parseFirstArtistId(response.payload().get().utf8());
          Request topTracksRequest = Request.forUri(
              String.format("%s/%s/top-tracks?country=%s", ARTIST_API, topArtistId, country));
          return client.send(topTracksRequest);
        })
        .thenApplyAsync(response -> Response.ok()
            .withPayload(parseTopTracks(response.payload().get().utf8())))
        // This .exceptionally doesn't provide any additional value,
        // but it shows how you could handle exceptions
        .exceptionally(throwable -> Response
            .forStatus(Status.INTERNAL_SERVER_ERROR.withReasonPhrase("Something failed")));
  }

  /**
   * Parses an artist top tracks response from the
   * <a href="https://developer.spotify.com/web-api/get-artists-top-tracks/">Spotify API</a>
   *
   * @param json The json response
   * @return A list of top tracks
   */
  private ArrayList<Track> parseTopTracks(String json) {
    ArrayList<Track> tracks = new ArrayList<>();
    try {
      JsonNode jsonNode = this.objectMapper.readTree(json);
      for (JsonNode trackNode : jsonNode.get("tracks")) {
        JsonNode albumNode = trackNode.get("album");
        String albumName = albumNode.get("name").asText();
        String artistName = trackNode.get("artists").get(0).get("name").asText();
        String trackName = trackNode.get("name").asText();

        tracks.add(new Track(trackName, new Album(albumName, new Artist(artistName))));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
    return tracks;
  }

  /**
   * Parses the first artist id from a JSON response from a
   * <a href="https://developer.spotify.com/web-api/search-item/">Spotify API search query</a>.
   *
   * @param json The json response
   * @return The id of the first artist in the response. null if response was empty.
   */
  private String parseFirstArtistId(String json) {
    try {
      JsonNode jsonNode = this.objectMapper.readTree(json);
      for (JsonNode node : jsonNode.get("artists").get("items")) {
        return node.get("id").asText();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
    return null;
  }
}
