package com.spotify.apollo.route;

import com.google.common.util.concurrent.FutureCallback;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Defines a function that can be used to share functionality among routes using
 * {@link Route#with(Middleware, String, String, Object)}.
 * It also allows to compose several functions (middlewares).
 */
@FunctionalInterface
public interface Middleware<H, T> extends Function<H, T> {

  default <K> Middleware<H, K> and(Middleware<? super T, ? extends K> other) {
    return h -> other.apply(apply(h));
  }

  static <T> AsyncHandler<T> syncToAsync(SyncHandler<T> handler) {
    return requestContext -> completedFuture(handler.invoke(requestContext));
  }

  static <T> AsyncHandler<T> guavaToAsync(ListenableFutureHandler<T> listenableFutureHandler) {
    return requestContext -> {
      CompletableFuture<T> future = new CompletableFuture<>();

      addCallback(
          listenableFutureHandler.invoke(requestContext),
          new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
              future.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
              future.completeExceptionally(t);
            }
          });

      return future;
    };
  }

}
