package com.spotify.apollo.route;

import com.google.auto.value.AutoValue;

import java.util.Optional;

import javax.annotation.Nullable;

import static com.spotify.apollo.route.Route.DocString.doc;

/**
 * The Route defines an endpoint to the service. There are basically two types of routes based
 * on the type of request handler, synchronous and asynchronous. Each route, apart from defining
 * the basic information like method, uri and handler, can have a reply content type, serializer,
 * documentation string, etc.
 *
 * A route handler might return a Response (or a ListenableFuture), which is a wrapper where you can
 * specify extra information about the reply, or any other type, that will be serialized
 * with AutoSerializer by default or with the explicitly specified one.
 *
 * If the handler returns a Response apollo will unwrap it and use the extra information that can be
 * specified, like headers, and reply with the given status code and serializeed payload if it exists.
 */
public interface Route<H> {

  String method();
  String uri();
  H handler();

  Optional<DocString> docString();

  ///////////////////////////////////////////////////////

  default <K> Route<K> withHandler(K handler) {
    return copy(
        method(), uri(), handler, docString().orElse(null));
  }

  default Route<H> withDocString(String summary, String description) {
    return copy(
        method(), uri(), handler(), doc(summary, description));
  }

  default <K> Route<K> withMiddleware(Middleware<? super H, ? extends K> middleware) {
    return copy(
        method(), uri(), middleware.apply(handler()), docString().orElse(null));
  }

  default Route<H> withPrefix(String prefix) {
    return copy(
        method(), prefix + uri(), handler(), docString().orElse(null));
  }

  <K> Route<K> copy(
      String method,
      String uri,
      K handler,
      @Nullable DocString docString);

  static <H> Route<H> create(
      String method,
      String uri,
      H handler,
      @Nullable DocString docString) {
    return new AutoValue_RouteImpl<>(
        method,
        uri,
        handler,
        Optional.ofNullable(docString));
  }

  static <H> Route<H> create(String method, String uri, H handler) {
    return create(method, uri, handler, null);
  }

  /**
   * Allows creating routes with middleware and lambdas in a type-inference-proof way. This is
   * functionally equivalent to {@code Route.create(method, uri, handler).withMiddleware(m)},
   * but if the handler type isn't concrete, then Java's type inference cannot handle that option.
   */
  static <H, K> Route<K> with(Middleware<? super H, ? extends K> m, String method, String uri, H handler) {
    return create(method, uri, m.apply(handler));
  }

  static <T> Route<AsyncHandler<T>> async(String method, String uri, AsyncHandler<T> handler) {
    return create(method, uri, handler);
  }

  static <T> Route<AsyncHandler<T>> sync(String method, String uri, SyncHandler<T> handler) {
    return create(method, uri, handler).withMiddleware(Middleware::syncToAsync);
  }

  static <T> Route<AsyncHandler<T>> future(String method, String uri,
                                           ListenableFutureHandler<T> handler) {
    return create(method, uri, handler).withMiddleware(Middleware::guavaToAsync);
  }

  @AutoValue
  abstract class DocString {
    public abstract String summary();
    public abstract String description();

    public static DocString doc(String summary, String description) {
      return new AutoValue_Route_DocString(summary, description);
    }
  }
}
