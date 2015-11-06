package com.spotify.apollo.standalone.acceptance;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.SyncHandler;
import com.spotify.apollo.standalone.LoadingException;
import com.spotify.apollo.standalone.StandaloneService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import okio.ByteString;

import static com.spotify.apollo.Status.OK;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * A service implemented using the things that currently represent the 'blessed path', or the
 * latest-and-greatest version of the API.
 *
 * DONE:
 * - pick one of annotations or programmatic routes? probably want to keep supporting both.
 *   => picked programmatic only
 * - clean up and/or remove RequestContext. Maybe it should be RequestScope and include the message?
 *   => keeping it and calling it RequestContext for now.
 * - create Response API
 * - change Invokable to return Response
 * - define Status/StatusType/Family hierarchy with overrides for reason phrase
 * - implement Response
 * - consider changing Invokable to 'inline' InvokeArgs?
 * - figure out a way to handle default values for things like serialization:
 *   * maybe a toggle for the Environment?
 *   * maybe a new interface that something like the RoutesClass below could implement? That way,
 *     you could set a default for a group of related routes/invokables.
 *   * maybe as a sub-interface of Invokable? That way, people could let their handlers extend some
 *     app-specific class that would set the serializer.
 *   * maybe as an optional argument to the Route? That would sort of be hard to do DRY.
 * - Consider replacing RouteProvider with Iterable<Route>
 *
 * TODO:
 * - A more complex example of how things should best be done (grouping related resources into
 *   a RouteProvider class, etc.)
 */
public class BlessedPathService implements AppInit {

  private static final Logger LOG = LoggerFactory.getLogger(BlessedPathService.class);

  public static void main(String[] args) throws LoadingException {
    StandaloneService.boot(new BlessedPathService(), "ping", "run", "cloud", "-v");
  }

  @Override
  public void create(Environment environment) {
    LOG.info("starting");
    environment.routingEngine()
        .registerSafeRoutes(routes())
        .registerRoute(
            Route.sync("GET", "/route2/<arg>", new MyHandler("Direct"))) // will use the apollo default serializer
        .registerRoute(
            Route.async("GET", "/async2/<arg>", new MyAsyncHandler("Direct")));
  }

  public static Stream<Route<AsyncHandler<Response<ByteString>>>> routes() {
    return Stream.of(
        Route.sync("GET", "/route/<arg>", new MyHandler("RouteProvider"))
            .withMiddleware(serialize(stringResponseToByteString()))
            .withDocString("returns a really interesting thing",
                           "based on the parameters, in a synchronous way"),
        Route.async("GET", "/async/<arg>", new MyAsyncHandler("RouteProvider"))
            .withMiddleware(serialize(stringResponseToByteString())),
        Route.sync("GET", "/sync/<arg>", new BasicHandler("RouteProvider"))
            .withMiddleware(serialize(response -> Response.forPayload(ByteString.encodeUtf8(response)))));
  }

  private static Function<Response<String>, Response<ByteString>> stringResponseToByteString() {
    return response -> response.withPayload(response.payload().isPresent() ?
                                            ByteString.encodeUtf8(response.payload().get()) :
                                            null);
  }

  static <T> Middleware<AsyncHandler<T>, AsyncHandler<Response<ByteString>>> serialize(Function<T, Response<ByteString>> serializer) {
    return handler -> requestContext -> handler.invoke(requestContext).thenApply(serializer);
  }

  static class MyHandler implements SyncHandler<Response<String>> {
    private final String term;

    MyHandler(String term) {
      this.term = term;
    }

    @Override
    public Response<String> invoke(RequestContext requestContext) {
      return Response.of(
          OK.withReasonPhrase("this really is A-OK"),
          term + ":route " + requestContext.pathArgs().get("arg"));
    }
  }

  static class MyAsyncHandler implements AsyncHandler<Response<String>> {
    private final String term;

    MyAsyncHandler(String term) {
      this.term = term;
    }

    @Override
    public CompletionStage<Response<String>> invoke(final RequestContext requestContext) {
      return asyncRequest(requestContext.pathArgs().get("arg"))
          .thenApply(input -> Response
              .forPayload(term + ":route " + input)
              .withHeader("X-MyHeader", input));
    }

    private static CompletionStage<String> asyncRequest(String arg) {
      return completedFuture(arg);
    }
  }

  static class BasicHandler implements SyncHandler<String> {
    private final String term;

    BasicHandler(String term) {
      this.term = term;
    }

    @Override
    public String invoke(RequestContext requestContext) {
      return term + ":route " + requestContext.pathArgs().get("arg");
    }
  }
}
